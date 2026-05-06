package bbmovie.transcode.temporal_orchestrator.nats;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.dto.UploadPurpose;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioEventParser {

    private static final Map<String, UploadPurpose> PURPOSE_MAP = Map.of(
            "MOVIE_SOURCE", UploadPurpose.MOVIE_SOURCE,
            "MOVIE_TRAILER", UploadPurpose.MOVIE_TRAILER,
            "MOVIE_POSTER", UploadPurpose.MOVIE_POSTER,
            "USER_AVATAR", UploadPurpose.USER_AVATAR
    );

    private final ObjectMapper objectMapper;

    /**
     * Parses every entry in SNS/S3 {@code Records[]} so batched notifications are not dropped.
     * Malformed or skipped records are logged and omitted; returning an empty list is expected for no usable jobs.
     */
    public List<TranscodeJobInput> parseAll(byte[] payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode records = root.path("Records");
            if (records.isMissingNode() || !records.isArray() || records.isEmpty()) {
                return List.of();
            }
            List<TranscodeJobInput> out = new ArrayList<>(records.size());
            for (JsonNode record : records) {
                try {
                    parseRecord(record).ifPresent(out::add);
                } catch (Exception e) {
                    log.warn("Skipping MinIO notification record: {}", e.getMessage());
                }
            }
            return Collections.unmodifiableList(out);
        } catch (Exception e) {
            log.error("Parse error: {}", e.getMessage());
            return List.of();
        }
    }

    private Optional<TranscodeJobInput> parseRecord(JsonNode record) {
        String eventName = record.path("eventName").asText("");
        if (!eventName.startsWith("s3:ObjectCreated:")) {
            return Optional.empty();
        }
        JsonNode s3Node = record.path("s3");
        String bucket = s3Node.path("bucket").path("name").asText("");
        String rawKey = s3Node.path("object").path("key").asText("");
        String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);

        JsonNode userMetadata = s3Node.path("object").path("userMetadata");
        String purposeStr = extractMetadataValue(userMetadata,
                "X-Amz-Meta-Purpose", "x-amz-meta-purpose", "purpose", "Purpose");
        String uploadId = extractMetadataValue(userMetadata,
                "X-Amz-Meta-Upload-Id", "x-amz-meta-upload-id", "upload-id", "uploadId",
                "X-Amz-Meta-Video-Id", "x-amz-meta-video-id", "video-id", "videoId");

        if (purposeStr == null || uploadId == null) {
            log.warn("Missing metadata purpose={} uploadId={}", purposeStr, uploadId);
            return Optional.empty();
        }

        UploadPurpose purpose = PURPOSE_MAP.get(purposeStr.toUpperCase());
        if (purpose == null) {
            log.warn("Unknown purpose {}", purposeStr);
            return Optional.empty();
        }

        long size = -1L;
        JsonNode sizeNode = s3Node.path("object").path("size");
        if (sizeNode.isNumber()) {
            size = sizeNode.asLong(-1L);
        }

        String contentType = extractMetadataValue(userMetadata,
                "Content-Type", "content-type", "X-Amz-Meta-Content-Type", "x-amz-meta-content-type");
        if (contentType == null) {
            contentType = "";
        }

        return Optional.of(new TranscodeJobInput(uploadId, bucket, key, purpose, contentType, size));
    }

    private static String extractMetadataValue(JsonNode metadata, String... keys) {
        for (String k : keys) {
            JsonNode value = metadata.path(k);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }
}
