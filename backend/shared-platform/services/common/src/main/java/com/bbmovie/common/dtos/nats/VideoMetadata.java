package com.bbmovie.common.dtos.nats;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public class VideoMetadata {

    private Long movieId;
    private String title;
    private String description;
    private List<String> categories;
    private String country;
    private String movieType;
    private Instant releaseDate;
    private String videoUrl;
    private String videoPublicId;
    private String trailerUrl;
    private String trailerPublicId;
    private Set<String> videoQuality;
    private Integer durationMinutes;
    private String posterUrl;
    private String posterPublicId;
    private Boolean active;

    public VideoMetadata() {}

    public VideoMetadata(
            Long movieId,
            String title,
            String description,
            List<String> categories,
            String country,
            String movieType,
            Instant releaseDate,
            String videoUrl,
            String videoPublicId,
            String trailerUrl,
            String trailerPublicId,
            Set<String> videoQuality,
            Integer durationMinutes,
            String posterUrl,
            String posterPublicId,
            Boolean active
    ) {
        this.movieId = movieId;
        this.title = title;
        this.description = description;
        this.categories = categories;
        this.country = country;
        this.movieType = movieType;
        this.releaseDate = releaseDate;
        this.videoUrl = videoUrl;
        this.videoPublicId = videoPublicId;
        this.trailerUrl = trailerUrl;
        this.trailerPublicId = trailerPublicId;
        this.videoQuality = videoQuality;
        this.durationMinutes = durationMinutes;
        this.posterUrl = posterUrl;
        this.posterPublicId = posterPublicId;
        this.active = active;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getMovieType() {
        return movieType;
    }

    public void setMovieType(String movieType) {
        this.movieType = movieType;
    }

    public Instant getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Instant releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoPublicId() {
        return videoPublicId;
    }

    public void setVideoPublicId(String videoPublicId) {
        this.videoPublicId = videoPublicId;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getTrailerPublicId() {
        return trailerPublicId;
    }

    public void setTrailerPublicId(String trailerPublicId) {
        this.trailerPublicId = trailerPublicId;
    }

    public Set<String> getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(Set<String> videoQuality) {
        this.videoQuality = videoQuality;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getPosterPublicId() {
        return posterPublicId;
    }

    public void setPosterPublicId(String posterPublicId) {
        this.posterPublicId = posterPublicId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public static class Builder {
        private Long movieId;
        private String title;
        private String description;
        private List<String> categories;
        private String country;
        private String movieType;
        private Instant releaseDate;
        private String videoUrl;
        private String videoPublicId;
        private String trailerUrl;
        private String trailerPublicId;
        private Set<String> videoQuality;
        private Integer durationMinutes;
        private String posterUrl;
        private String posterPublicId;
        private Boolean active;

        public Builder movieId(Long movieId) { this.movieId = movieId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder categories(List<String> categories) { this.categories = categories; return this; }
        public Builder country(String country) { this.country = country; return this; }
        public Builder movieType(String movieType) { this.movieType = movieType; return this; }
        public Builder releaseDate(Instant releaseDate) { this.releaseDate = releaseDate; return this; }
        public Builder videoUrl(String videoUrl) { this.videoUrl = videoUrl; return this; }
        public Builder videoPublicId(String videoPublicId) { this.videoPublicId = videoPublicId; return this; }
        public Builder trailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; return this; }
        public Builder trailerPublicId(String trailerPublicId) { this.trailerPublicId = trailerPublicId; return this; }
        public Builder videoQuality(Set<String> videoQuality) { this.videoQuality = videoQuality; return this; }
        public Builder durationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public Builder posterUrl(String posterUrl) { this.posterUrl = posterUrl; return this; }
        public Builder posterPublicId(String posterPublicId) { this.posterPublicId = posterPublicId; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }

        public VideoMetadata build() {
            return new VideoMetadata(
                    movieId, title, description, categories, country,
                    movieType, releaseDate, videoUrl, videoPublicId,
                    trailerUrl, trailerPublicId, videoQuality,
                    durationMinutes, posterUrl, posterPublicId, active
            );
        }
    }
}
