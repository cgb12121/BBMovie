package com.bbmovie.ai_assistant_service.core.high_level.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component("AdminTools")
public class AdminTools {
    @Tool(name = "Admin System Information", value = "This is a very sensitive information that can be access by admin")
    public String adminAgentInformation(@P("userRole") String userRole) {
        if (!"admin".equalsIgnoreCase(userRole)) {
            return "Access denied. Only administrators can view system information.";
        }
        return  "The admin system information is: Hello World with a lot of love";
    }

    @Tool("View user reports, accessible to mod and admin users.")
    public String viewUserReports(@P("userRole") String userRole) {
        if (!"mod".equalsIgnoreCase(userRole) && !"admin".equalsIgnoreCase(userRole)) {
            return "Access denied. Only moderators and administrators can view user reports.";
        }
        return "Displaying recent user reports: User 'X' reported 'spam' on movie 'Y'. User 'A' reported 'inappropriate content' on comment 'B'.";
    }

    @Tool("Temporarily disable comments for a movie, accessible to mod and admin users.")
    public String disableMovieComments(@P("movieTitle") String movieTitle, @P("userRole") String userRole) {
        if (!"mod".equalsIgnoreCase(userRole) && !"admin".equalsIgnoreCase(userRole)) {
            return "Access denied. Only moderators and administrators can disable movie comments.";
        }
        return "Comments for movie '" + movieTitle + "' have been temporarily disabled.";
    }

    @Tool("Manage user accounts (e.g., ban, unban), accessible only to admin users.")
    public String manageUserAccount(@P("username") String username, @P("action") String action, @P("userRole") String userRole) {
        if (!"admin".equalsIgnoreCase(userRole)) {
            return "Access denied. Only administrators can manage user accounts.";
        }
        return "User account '" + username + "' has been " + action + "d.";
    }

    @Tool("Deploy system updates, accessible only to admin users.")
    public String deploySystemUpdates(@P("version") String version, @P("userRole") String userRole) {
        if (!"admin".equalsIgnoreCase(userRole)) {
            return "Access denied. Only administrators can deploy system updates.";
        }
        return "System update to version " + version + " initiated.";
    }
}
