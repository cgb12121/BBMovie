package com.bbmovie.search.utils;

import io.qdrant.client.grpc.JsonWithInt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.qdrant.client.ValueFactory.nullValue;
import static io.qdrant.client.ValueFactory.value;

public class QdrantHelper {

    private QdrantHelper() {}

    public static JsonWithInt.Value objectToValue(Object o) {
        switch (o) {
            case null -> {
                return nullValue();
            }
            case String s -> {
                return value(s);
            }
            case Integer i -> {
                return value(i.longValue()); // Qdrant chỉ có Int64
            }
            case Long l -> {
                return value(l);
            }
            case Double d -> {
                return value(d);
            }
            case Float f -> {
                return value(f.doubleValue());
            }
            case Boolean b -> {
                return value(b);
            }
            case List<?> l -> {
                // Đệ quy cho List
                List<JsonWithInt.Value> list = l.stream()
                        .map(QdrantHelper::objectToValue)
                        .collect(Collectors.toList());
                return value(list); // ValueFactory.value(List<Value>)
            }
            case Map<?, ?> m -> {
                // Đệ quy cho Map (Nested Object)
                Map<String, JsonWithInt.Value> map = new HashMap<>();
                for (Map.Entry<?, ?> entry : m.entrySet()) {
                    map.put(entry.getKey().toString(), objectToValue(entry.getValue()));
                }
                return value(map); // ValueFactory.value(Map<String, Value>)
            }
            default -> {
            }
        }
        return nullValue(); // Fallback
    }
}
