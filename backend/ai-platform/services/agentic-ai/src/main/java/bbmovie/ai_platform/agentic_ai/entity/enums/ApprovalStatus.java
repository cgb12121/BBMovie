package bbmovie.ai_platform.agentic_ai.entity.enums;

import bbmovie.ai_platform.agentic_ai.exception.InvalidStatusTransitionException;

/**
 * Represents the status of a Human-in-the-Loop (HITL) approval request.
 * 
 * Transition rules:
 * - PENDING can move to APPROVED or REJECTED.
 * - APPROVED and REJECTED are terminal states.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ApprovalStatus approveIfPending(ApprovalStatus current) {
        if (current != PENDING) {
            throw new InvalidStatusTransitionException(current, APPROVED);
        }
        return APPROVED;
    }

    public static ApprovalStatus rejectIfPending(ApprovalStatus current) {
        if (current != PENDING) {
            throw new InvalidStatusTransitionException(current, REJECTED);
        }
        return REJECTED;
    }
}
