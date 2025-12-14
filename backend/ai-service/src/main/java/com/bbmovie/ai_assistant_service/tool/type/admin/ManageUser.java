package com.bbmovie.ai_assistant_service.tool.type.admin;

import com.bbmovie.ai_assistant_service.hitl.ActionType;
import com.bbmovie.ai_assistant_service.hitl.RequiresApproval;
import com.bbmovie.ai_assistant_service.hitl.RiskLevel;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

@SuppressWarnings("unused")
@Component
@Qualifier("adminTools")
public class ManageUser implements AdminTools {

    @Tool("Reads system configuration (Safe operation)")
    @RequiresApproval(action = ActionType.SENSITIVE_ACTION, baseRisk = RiskLevel.EXTREME, description = "Read system config")
    public String getSystemConfig() {
        return "System Config: [Debug: OFF, Cache: ON]";
    }

    @Tool("Updates user theme preference (Low risk)")
    @RequiresApproval(action = ActionType.UPDATE_RESOURCE, baseRisk = RiskLevel.LOW, description = "Update user theme")
    public String updateTheme(String userId, String themeColor) {
        return String.format("Updated theme for user %s to %s", userId, themeColor);
    }

    @Tool("Updates user nickname (Medium risk - Personal Info)")
    @RequiresApproval(action = ActionType.UPDATE_RESOURCE, baseRisk = RiskLevel.MEDIUM, description = "Update user nickname")
    public String updateNickname(String userId, String newNickname) {
        return String.format("Updated nickname for user %s to %s", userId, newNickname);
    }

    @Tool("Changes user email address (High risk - Identity)")
    @RequiresApproval(action = ActionType.UPDATE_RESOURCE, baseRisk = RiskLevel.HIGH, description = "Change user email")
    public String changeEmail(String userId, String newEmail) {
        return String.format("Changed email for user %s to %s", userId, newEmail);
    }

    @Tool("Deletes a user account (Extreme risk - Destructive)")
    @RequiresApproval(action = ActionType.DELETE_RESOURCE, baseRisk = RiskLevel.EXTREME, description = "Permanently delete user account")
    public String deleteUserAccount(String userId) {
        return String.format("PERMANENTLY DELETED user account: %s", userId);
    }

    @Tool("Drops a database table (Extreme risk - System Destruction)")
    @RequiresApproval(action = ActionType.DELETE_RESOURCE, baseRisk = RiskLevel.EXTREME, description = "Drop database table")
    public String dropTable(String tableName) {
        return String.format("DROPPED TABLE: %s", tableName);
    }

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