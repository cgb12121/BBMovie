package com.bbmovie.ai_assistant_service.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component("GuestTools")
public class GuestTools {

    @Tool("Get basic information about a movie by its title.")
    public String getMovieInfo(@P("movieTitle") String movieTitle) {
        // placeholder
        if (movieTitle.equalsIgnoreCase("Inception")) {
            return "Inception (2010) - A thief who steals corporate secrets through use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.";
        } else if (movieTitle.equalsIgnoreCase("The Matrix")) {
            return "The Matrix (1999) - A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.";
        }
        return "Could not find information for movie: " + movieTitle + ". Please try another title.";
    }

    @Tool("Search for movies by a given keyword.")
    public String searchMovies(@P("keyword") String keyword) {
        //placeholder
        if (keyword.equalsIgnoreCase("action")) {
            return "Popular action movies: Die Hard, The Dark Knight, Mad Max: Fury Road.";
        } else if (keyword.equalsIgnoreCase("comedy")) {
            return "Popular comedy movies: Superbad, The Hangover, Anchorman.";
        }
        return "No movies found for keyword: " + keyword + ". Try 'action' or 'comedy'.";
    }
}
