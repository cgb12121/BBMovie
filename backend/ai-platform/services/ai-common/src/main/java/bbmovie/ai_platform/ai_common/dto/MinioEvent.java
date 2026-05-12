package bbmovie.ai_platform.ai_common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinioEvent {
    private List<MinioRecord> records;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MinioRecord {
        private String eventName;
        private S3Metadata s3;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S3Metadata {
        private BucketInfo bucket;
        private ObjectInfo object;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BucketInfo {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectInfo {
        private String key;
        private Long size;
        private String contentType;
    }
}
