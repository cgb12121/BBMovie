package com.example.bbmovie.service.elasticsearch.sample;

import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class SampleData {

    private static final String[] MOVIE_TITLES = {
            "The Matrix", "Inception", "Interstellar", "The Shawshank Redemption", "Pulp Fiction",
            "The Godfather", "The Dark Knight", "Fight Club", "Forrest Gump", "The Lord of the Rings",
            "Titanic", "Avatar", "Jurassic Park", "Star Wars", "The Avengers", "Black Panther",
            "Parasite", "Whiplash", "The Social Network", "La La Land"
    };

    private static final String[] MOVIE_DESCRIPTIONS = {
            "A mind-bending journey through reality and illusion.",
            "An epic tale of love and sacrifice across time and space.",
            "A story of redemption and hope in the darkest of places.",
            "Crime, family, and power in the underworld of organized crime.",
            "A hero rises to fight against injustice in a corrupt city.",
            "An underground club where men fight to feel alive.",
            "A simple man with a low IQ witnesses and influences key historical events.",
            "A quest to destroy a powerful ring and save Middle-earth.",
            "A tragic love story aboard the ill-fated RMS Titanic.",
            "A marine on an alien planet fights for survival and understanding.",
            "Scientists bring dinosaurs back to life with disastrous consequences.",
            "A galaxy far, far away with rebels fighting an evil empire.",
            "Earth's mightiest heroes assemble to face a powerful threat.",
            "The first black superhero in the Marvel Cinematic Universe.",
            "A poor family's cunning plan infiltrates a wealthy household.",
            "A young drummer pursues perfection at any cost.",
            "The story of how Facebook was created and the lawsuits that followed.",
            "A musician falls in love while pursuing her dreams in Los Angeles."
    };

    private static final String[] CATEGORIES = {
            "Action", "Adventure", "Comedy", "Drama", "Horror",
            "Sci-Fi", "Thriller", "Romance", "Fantasy", "Animation",
            "Documentary", "Crime", "Mystery", "Musical", "Western"
    };

    private static final String[] POSTER_URLS = {
            "https://fastly.picsum.photos/id/247/200/300.jpg?hmac=DOAWkFIrJUIvEj0t5qAsGiVgyTn8_e8EicBaXPCQge8",
            "https://fastly.picsum.photos/id/70/200/300.jpg?hmac=8-6v4fVxk6exesGT53s01yaJuediQIreacSHqZY3mV4",
            "https://fastly.picsum.photos/id/564/200/300.jpg?hmac=GML84ZsOUsd0_XLIMleR9RvRFT8-pojH0AwU7tRHoCg",
            "https://fastly.picsum.photos/id/200/200/300.jpg?hmac=XVCLpc2Ddr652IrKMt3L7jISDD4au5O9ZIr3fwBtxo8",
            "https://fastly.picsum.photos/id/53/200/300.jpg?hmac=KbEX4oNyVO15M-9S4xMsefrElB1uiO3BqnvVqPnhPgE",
            "https://fastly.picsum.photos/id/913/200/300.jpg?hmac=DjpzGA27POHBn03vW7UxM5gI9phMxuAZ4hSKcRfJD9Y",
            "https://fastly.picsum.photos/id/403/200/300.jpg?hmac=kgUsxOWk-ud5wENU54rY-VyaYzYYrlr4aoA3LhcG2Dc",
            "https://fastly.picsum.photos/id/987/200/300.jpg?hmac=JG_lwzlHFo64MDTTkaO_NK_KfCF-FE4ajdvEFqPJ4qY",
            "https://fastly.picsum.photos/id/88/200/300.jpg?hmac=JmiMN7iyW4Saka82S4HzDvbOjMSB2k9NwTN29MHWqa4",
            "https://fastly.picsum.photos/id/11/200/300.jpg?hmac=n9AzdbWCOaV1wXkmrRfw5OulrzXJc0PgSFj4st8d6ys"
    };

    public static List<MovieVectorDocument> getSampleVectorDocuments() {
        List<MovieVectorDocument> documents = new ArrayList<>();
        Random random = new Random();

        IntStream.range(0, 100).forEach(i -> {
            float[] contentVector = new float[384];
            for (int j = 0; j < contentVector.length; j++) {
                contentVector[j] = random.nextFloat();
            }

            int numCategories = 1 + random.nextInt(3);
            List<String> categories = new ArrayList<>();
            for (int k = 0; k < numCategories; k++) {
                String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
                if (!categories.contains(category)) {
                    categories.add(category);
                }
            }

            int year = 1970 + random.nextInt(54);

            documents.add(createMovieVectorDocument(
                    String.valueOf(i + 1),
                    generateUniqueTitle(i, random),
                    MOVIE_DESCRIPTIONS[random.nextInt(MOVIE_DESCRIPTIONS.length)],
                    contentVector,
                    1.0 + random.nextDouble() * 4.0,
                    categories,
                    POSTER_URLS[random.nextInt(POSTER_URLS.length)],
                    generateRandomDate(year)
            ));
        });

        return documents;
    }

    private static String generateUniqueTitle(int index, Random random) {
        String baseTitle = MOVIE_TITLES[random.nextInt(MOVIE_TITLES.length)];
        return baseTitle + " " + (index % 10 == 0 ? "" :
                (random.nextBoolean() ? "Part " + (1 + random.nextInt(3)) :
                        "The " + (char)('A' + random.nextInt(26)) + " Version"));
    }

    private static LocalDateTime generateRandomDate(int year) {
        int month = 1 + ThreadLocalRandom.current().nextInt(12);
        int day = 1 + ThreadLocalRandom.current().nextInt(28);
        return LocalDateTime.of(year, month, day, 0, 0);
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