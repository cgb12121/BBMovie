package bbmovie.ai_platform.agentic_ai.exception;

import bbmovie.ai_platform.agentic_ai.entity.enums.ApprovalStatus;

/**
 * Thrown when an illegal transition is attempted on an approval request.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(ApprovalStatus current, ApprovalStatus target) {
        super(String.format("Cannot transition approval request from %s to %s", current, target));
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
