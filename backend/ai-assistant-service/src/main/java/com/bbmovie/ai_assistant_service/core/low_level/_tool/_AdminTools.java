package com.bbmovie.ai_assistant_service.core.low_level._tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Qualifier("_AdminTool")
@SuppressWarnings("unused")
public class _AdminTools implements _AiTools {

    @Tool("Manages a user account, allowing actions like banning, unbanning, or changing roles.")
    public void manageUserAccount(
            @P("The unique identifier of the target user.") UUID userId,
            @P("The administrative action to perform. Can be 'ban', 'unban', 'promoteToMod', 'demoteFromMod'.") String action,
            @P("Optional: The duration for a ban, e.g., '7d' or 'permanent'.") String duration,
            @P("A mandatory reason for the action, for audit purposes.") String reason
    ) {
        // Implementation pending
    }

    @Tool("Views sensitive, non-public details for a user account, such as email and account status.")
    public void viewUserDetails(@P("The unique identifier of the user to view.") UUID userId) {
        // Implementation pending
    }

    @Tool("Retrieves system-wide usage statistics for a given time period.")
    public void getSystemUsageStatistics(@P("The time period for the report. Can be 'daily', 'weekly', or 'monthly'.") String timePeriod) {
        // Implementation pending
    }

    @Tool("Views the audit trail of actions taken by moderators and administrators.")
    public void viewAuditTrail(
            @P("Optional: Filter by the user who performed the action.") UUID userId,
            @P("Optional: Filter by the type of action, e.g., 'ban'.") String actionType
    ) {
        // Implementation pending
    }

    @Tool("Broadcasts a system-wide message or announcement to all users.")
    public void broadcastSystemMessage(
            @P("The content of the message to broadcast.") String message,
            @P("The severity level of the message. Can be 'info' or 'critical'.") String level
    ) {
        // Implementation pending
    }
}