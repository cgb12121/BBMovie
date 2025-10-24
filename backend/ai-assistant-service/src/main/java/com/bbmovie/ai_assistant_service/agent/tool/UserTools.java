package com.bbmovie.ai_assistant_service.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@SuppressWarnings("unused")
@Component("UserTools")
public class UserTools {

    private final WebClient webClient;
    private final Environment env;

    @Autowired
    public UserTools(WebClient webClient, Environment env) {
        this.webClient = webClient;
        this.env = env;
    }

    @Tool("Get information about this AI assistant (name, purpose, capabilities). Safe for all users.")
    public String getAgentInfo() {
        return """
            I'm Qwen, the BBMovie Assistant.
            - Purpose: Help you find movies, actors, and recommendations.
            - Powered by: Ollama (qwen3:0.6b) + OMDb API
            - I cannot answer non-movie questions.
            - For support, contact admin@bbmovie.com
            """;
    }

    @Tool("Explain what the user can ask this assistant.")
    public String getCapabilities() {
        return """
        You can ask me:
        • "Recommend horror movies"
        • "Movies with Tom Hanks"
        • "Best sci-fi from the 2010s"
        • "What can you do?"

        I cannot:
        • Answer math, coding, or weather questions
        • Access real-time data
        • Book tickets or showtime

        All recommendations are based on curated lists.
        """;
    }

    @Tool("Get top movie recommendations for a specific genre (e.g., horror, comedy, sci-fi) based on user tier.")
    public String recommendByGenre(@P("genre") String genre, @P("userTier") String userTier) {
        if (genre == null || genre.isBlank()) {
            return "Please specify a genre (e.g., 'horror', 'romance', 'action').";
        }

        String recommendations;
        switch (genre.toLowerCase()) {
            case "horror":
                recommendations = """
                Top horror movies:
                - The Exorcist (1973)
                - Hereditary (2018)
                - Get Out (2017)
                - The Shining (1980)
                """;
                break;
            case "comedy":
                recommendations = """
                Top comedies:
                - Superbad (2007)
                - Bridesmaids (2011)
                - The Grand Budapest Hotel (2014)
                """;
                break;
            case "sci-fi":
                recommendations = """
                Top sci-fi:
                - Blade Runner 2049 (2017)
                - Interstellar (2014)
                - Arrival (2016)
                """;
                break;
            default:
                return "I can recommend movies for horror, comedy, or sci-fi. Try one of those!";
        }

        if ("premium".equalsIgnoreCase(userTier)) {
            recommendations += "\n\nExclusive Premium Recommendations: The Babadook (2014), Under the Skin (2013)";
        } else if ("basic".equalsIgnoreCase(userTier)) {
            recommendations += "\n\nAdditional Basic Recommendations: A Quiet Place (2018), Don't Breathe (2016)";
        }
        return recommendations;
    }

    @Tool("Get detailed information about a movie by its title, accessible to basic and premium users.")
    public String getDetailedMovieInfo(@P("movieTitle") String movieTitle, @P("userTier") String userTier) {
        if (!"basic".equalsIgnoreCase(userTier) && !"premium".equalsIgnoreCase(userTier)) {
            return "Detailed movie information is only available for Basic and Premium users.";
        }
        // In a real application, this would call an external movie API (e.g., OMDb)
        // For now, we'll return a placeholder.
        if (movieTitle.equalsIgnoreCase("Inception")) {
            return "Inception (2010) - Directed by Christopher Nolan. Starring Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page. Plot: A thief who steals corporate secrets through use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O. IMDb Rating: 8.8/10.";
        } else if (movieTitle.equalsIgnoreCase("The Matrix")) {
            return "The Matrix (1999) - Directed by The Wachowskis. Starring Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss. Plot: A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers. IMDb Rating: 8.7/10.";
        }
        return "Could not find detailed information for movie: " + movieTitle + ".";
    }

    @Tool("Get exclusive behind-the-scenes content for a movie, accessible only to premium users.")
    public String getExclusiveContent(@P("movieTitle") String movieTitle, @P("userTier") String userTier) {
        if (!"premium".equalsIgnoreCase(userTier)) {
            return "Exclusive content is only available for Premium users.";
        }
        // Placeholder for exclusive content
        if (movieTitle.equalsIgnoreCase("Inception")) {
            return "Inception (2010) Exclusive: Behind-the-scenes footage reveals how the rotating hallway scene was filmed using a massive, custom-built set that could rotate 360 degrees.";
        }
        return "No exclusive content found for movie: " + movieTitle + ".";
    }

    @Tool("Ask the user to clarify their movie request if it's vague or incomplete.")
    public String requestClarification(@P("example") String exampleQuery) {
        return "Could you be more specific? For example: \"" + exampleQuery + "\"";
    }

    @Tool("Search the internet for current, factual information. Returns a summarized answer.")
    public String tavilySearch(@P("query") String query) {
        try {
            String tavilyApiKey = env.getProperty("tavily.api-key", "");
            if (tavilyApiKey.isBlank()) {
                return "Search is not available now. Please try again later.";
            }
            String response = webClient.post()
                    .uri("https://api.tavily.com/search")
                    .bodyValue(Map.of(
                            "api_key", tavilyApiKey,
                            "query", query,
                            "search_depth", "basic",
                            "include_answer", true
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(node -> node.path("answer").asText("No answer found."))
                    .block(Duration.ofSeconds(5));

            return response != null ? response : "No answer found.";
        } catch (Exception e) {
            return "Web search failed.";
        }
    }
}