package bbmovie.ai_platform.ai_common.enums;

import lombok.Getter;

@Getter
public enum AiStatus {
    SUCCESS(200, "Success"),
    INGESTION_PENDING(1001, "Ingestion in progress"),
    INGESTION_FAILED(1002, "Ingestion failed"),
    NOT_FOUND(404, "Resource not found"),
    INTERNAL_ERROR(500, "Internal server error");

    private final int code;
    private final String reason;

    AiStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public static AiStatus fromCode(int code) {
        for (AiStatus status : values()) {
            if (status.code == code) return status;
        }
        return INTERNAL_ERROR;
    }
}
