package com.bbmovie.ai_assistant_service.tool.type.user;

import com.bbmovie.ai_assistant_service.tool.AiTools;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Qualifier("userTools")
@SuppressWarnings("unused")
public class UserTools implements AiTools {

    @Tool("Searches for movies by title, actor, or genre.")
    public void searchMovies(
            @P("The search query, e.g., 'Inception' or 'Tom Hanks'") String query,
            @P("The field to search by. Can be 'title', 'actor', or 'genre'.") String searchBy
    ) {
        // Implementation pending
    }

    @Tool("Retrieves detailed information for a specific movie, such as plot, cast, ratings, and release year.")
    public void getMovieDetails(@P("The unique identifier of the movie.") UUID movieId) {
        // Implementation pending
    }

    @Tool("Provides personalized or curated movie recommendations based on genre or other criteria.")
    public void getRecommendations(
            @P("The genre to getWithCursor recommendations for, e.g., 'horror'.") String genre,
            @P("Optional: The decade to filter by, e.g., '1990s'.") String decade
    ) {
        // Implementation pending
    }

    @Tool("Displays the list of movies the current user has saved to their personal watchlist.")
    public void viewMyWatchlist() {
        // Implementation pending
    }

    @Tool("Adds a movie to the user's personal watchlist.")
    public void addToWatchlist(@P("The unique identifier of the movie to add.") UUID movieId) {
        // Implementation pending
    }

    @Tool("Removes a movie from the user's personal watchlist.")
    public void removeFromWatchlist(@P("The unique identifier of the movie to remove.") UUID movieId) {
        // Implementation pending
    }

    @Tool("Submits a new movie review and rating from the user.")
    public void submitReview(
            @P("The unique identifier of the movie being reviewed.") UUID movieId,
            @P("The user's rating on a scale of 1 to 5.") int rating,
            @P("The text content of the user's review.") String reviewText
    ) {
        // Implementation pending
    }
}
