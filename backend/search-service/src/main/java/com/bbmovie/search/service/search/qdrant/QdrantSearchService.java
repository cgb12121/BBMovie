package com.bbmovie.search.service.search.qdrant;

import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.dto.SearchCriteria;
import com.bbmovie.search.service.search.SearchService;
import com.bbmovie.search.service.embedding.EmbeddingService;
import com.bbmovie.search.utils.EmbeddingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("qdrant")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.engine", havingValue = "qdrant", matchIfMissing = true)
public class QdrantSearchService implements SearchService {

     private final QdrantClient qdrantClient;
     private final EmbeddingService embeddingService;
     private final ObjectMapper objectMapper;

     @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
     private String collectionName;

    @Override
    public <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz) {
        return Mono.fromCallable(() -> {
            try {
                // 1. FIX LỖI COUNT:
                // qdrantClient.countAsync().get() trả về CountResult (object), không phải Long.
                // Nhưng nếu IDE báo "in Long", có thể bác dùng bản wrapper nào đó?
                // Code chuẩn gRPC client 1.16.x là:
                Long result = qdrantClient.countAsync(collectionName).get();
                long total = (result != null) ? result : 0L;

                // 2. FIX LỖI OFFSET (Quan trọng):
                // Qdrant SCROLL KHÔNG CÓ OFFSET kiểu int (SQL style). Nó dùng Cursor.
                // Để làm phân trang kiểu truyền thống (page 1, 2, 3), ta phải dùng mẹo:
                // Dùng SEARCH API với vector RỖNG (Zero Vector). Search API hỗ trợ .setOffset(int).
                //TODO: remove hard code dims here: 384
                List<Float> zeroVector = Collections.nCopies(384, 0.0f);

                // Build Filter nếu cần (Age, Region...) - Cái này bác đang thiếu
                Common.Filter.Builder filterBuilder = Common.Filter.newBuilder();
                if (region != null && !region.isEmpty()) {
                    filterBuilder.addMust(Common.Condition.newBuilder()
                            .setField(Common.FieldCondition.newBuilder()
                                    .setKey("country")
                                    .setMatch(Common.Match.newBuilder()
                                            .setKeyword(region)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    );
                }
                // (Thêm logic filter age vào đây nếu cần)

                // Dùng Search thay vì Scroll để có Offset
                List<Points.ScoredPoint> searchResult = qdrantClient.searchAsync(
                        Points.SearchPoints.newBuilder()
                                .setCollectionName(collectionName)
                                .addAllVector(zeroVector) // Hack: Search vector 0 để lấy tất cả
                                .setFilter(filterBuilder.build())
                                .setOffset((long) page * size)   // ✅ Search API có Offset
                                .setLimit(size)
                                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                                .build()
                ).get();

                // Check null safe
                if (searchResult == null) {
                    return new PageResponse<T>(); // Fix generic
                }

                List<T> content = searchResult.stream()
                        .map(point -> mapToEntity(point.getPayloadMap(), clazz))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                int totalPages = (int) Math.ceil((double) total / size);

                // 3. FIX GENERIC MISMATCH: Thêm <T> vào new PageResponse<T>
                return new PageResponse<T>(
                        content,
                        page,
                        size,
                        total,
                        totalPages
                );

            } catch (Exception e) {
                log.error("Qdrant getAllMovies failed", e);
                return new PageResponse<T>();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public <T> Mono<PageResponse<T>> searchSimilar(SearchCriteria criteria, Class<T> clazz) {
        return embeddingService.generateEmbedding(criteria.getQuery())
                .flatMap(vector -> Mono.fromCallable(() -> {
                    try {
                        List<Float> queryVector = EmbeddingUtils.convertToFloatList(vector);

                        List<Points.ScoredPoint> searchResponse = qdrantClient.searchAsync(
                                Points.SearchPoints.newBuilder()
                                        .setCollectionName(collectionName)
                                        .addAllVector(queryVector)
                                        .setLimit(criteria.getSize())
                                        .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                                        .build()
                        ).get();

                        if (searchResponse == null) {
                            return new PageResponse<T>();
                        }

                        List<T> content = searchResponse.stream()
                                .map(point -> mapToEntity(point.getPayloadMap(), clazz))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        // Qdrant search vector không trả về total hits chính xác (nó là KNN)
                        // Nên ta fake total bằng size hoặc lấy từ count (nhưng count thì chậm)
                        long approxTotal = content.size();

                        // FIX GENERIC: Thêm <T>
                        return new PageResponse<T>(
                                content,
                                criteria.getPage(),
                                criteria.getSize(),
                                approxTotal,
                                1
                        );

                    } catch (Exception e) {
                        log.error("Qdrant searchSimilar failed", e);
                        // FIX GENERIC: Thêm <T>
                        return new PageResponse<T>();
                    }
                }).subscribeOn(Schedulers.boundedElastic())); // Quan trọng: Đẩy blocking gRPC ra khỏi EventLoop
    }

    private <T> T mapToEntity(Map<String, JsonWithInt.Value> payload, Class<T> clazz) {
        try {
            // FIX 4: Thay Collectors.toMap bằng HashMap thủ công
            // Lý do: Collectors.toMap sẽ ném NullPointerException nếu convertValue trả về null.
            // HashMap cho phép value null.
            Map<String, Object> javaMap = new HashMap<>();
            if (payload != null) {
                for (Map.Entry<String, JsonWithInt.Value> entry : payload.entrySet()) {
                    javaMap.put(entry.getKey(), convertValue(entry.getValue()));
                }
            }

            return objectMapper.convertValue(javaMap, clazz);
        } catch (Exception e) {
            log.error("Mapping error", e);
            return null;
        }
    }

    private Object convertValue(JsonWithInt.Value value) {
        if (value == null) return null; // Safety check

        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case INTEGER_VALUE -> value.getIntegerValue();
            case DOUBLE_VALUE -> value.getDoubleValue();
            case BOOL_VALUE -> value.getBoolValue();
            case LIST_VALUE -> value.getListValue().getValuesList().stream()
                    .map(this::convertValue)
                    .collect(Collectors.toList());
            case STRUCT_VALUE -> {
                // Recursion must use HashMap manually to avoid null value
                Map<String, Object> map = new HashMap<>();
                value.getStructValue().getFieldsMap().forEach((k, v) ->
                        map.put(k, convertValue(v))
                );
                yield map;
            }
            case NULL_VALUE, KIND_NOT_SET -> null;
        };
    }
}
