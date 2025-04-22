package com.example.bbmovie.service.elasticsearch.sample;

import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SampleData {

    public static List<MovieVectorDocument> getSampleVectorDocuments() {
        List<Float> hardcodedVector = new ArrayList<>();
        for (int i = 0; i < 384; i++) {
            hardcodedVector.add(0.1f);
        }
        
        return List.of(
                createMovieVectorDocument(
                        "1",
                        "The Matrix",
                        "A computer hacker learns about the true nature of reality and his role in the war against its controllers.",
                        convertListToArray(hardcodedVector),
                        3.0,
                        List.of("Tralalelo Tralala"),
                        "no img",
                        LocalDateTime.of(2001, 1, 1, 0, 0)
                ),
                createMovieVectorDocument(
                        "2",
                        "Forrest Gump",
                        "The presidencies of Kennedy and Johnson, the Vietnam War, the Watergate scandal and other historical events unfold from the perspective of an Alabama man with an IQ of 75.",
                        convertListToArray(hardcodedVector),
                        2.0,
                        List.of("Tung Tung Tung Sahur"),
                        "no  here",
                        LocalDateTime.of(2005, 1, 1, 0, 0)
                )
        );
    }

    private static float[] convertListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static MovieVectorDocument createMovieVectorDocument(
            String id,
            String title,
            String description,
            float[] contentVector,
            Double rating,
            List<String> categories,
            String posterUrl,
            LocalDateTime releaseDate
    ) {
        return MovieVectorDocument.builder()
                .id(id)
                .title(title)
                .description(description)
                .contentVector(contentVector)
                .rating(rating)
                .categories(categories)
                .posterUrl(posterUrl)
                .releaseDate(releaseDate)
                .build();
    }
}
