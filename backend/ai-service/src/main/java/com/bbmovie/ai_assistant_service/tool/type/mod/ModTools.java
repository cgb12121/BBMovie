package com.bbmovie.ai_assistant_service.tool.type.mod;

import com.bbmovie.ai_assistant_service.tool.AiTools;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Qualifier("modTool")
@SuppressWarnings("unused")
public class ModTools implements AiTools {

    @Tool("Views a queue of content (reviews, comments) that has been reported by users.")
    public void viewReportedContent(
            @P("The type of content to view. Can be 'review' or 'comment'.") String contentType,
            @P("The status of the reports to view. Can be 'new' or 'resolved'.") String status
    ) {
        // Implementation pending
    }

    @Tool("Retrieves the full text and context of a single piece of reported content.")
    public void getContentView(
            @P("The unique identifier of the content.") UUID contentId,
            @P("The type of content, e.g., 'review'.") String contentType
    ) {
        // Implementation pending
    }

    @Tool("Takes a moderation action on a piece of content, such as approving or hiding it.")
    public void moderateContent(
            @P("The unique identifier of the content.") UUID contentId,
            @P("The action to take. Can be 'approve', 'hide', or 'delete'.") String action,
            @P("The reason for the moderation action, for audit purposes.") String reason
    ) {
        // Implementation pending
    }

    @Tool("Views the public posting history of a specific user to check for patterns of abuse.")
    public void viewUserPostHistory(@P("The unique identifier of the user.") UUID userId) {
        // Implementation pending
    }

    @Tool("Sends a predefined warning message to a user for violating community guidelines.")
    public void sendWarningToUser(
            @P("The unique identifier of the user to warn.") UUID userId,
            @P("The warning message to send.") String warningMessage
    ) {
        // Implementation pending
    }
}