package com.bbmovie.droolsengine.dto;

/**
 * Fact object representing a university registry match for Drools evaluation.
 */
public class UniversityMatch {
    private String name;
    private String domain;
    private String country;
    private double confidence;
    private boolean matched;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UniversityMatch match = new UniversityMatch();

        public Builder name(String name) {
            match.setName(name);
            return this;
        }

        public Builder domain(String domain) {
            match.setDomain(domain);
            return this;
        }

        public Builder country(String country) {
            match.setCountry(country);
            return this;
        }

        public Builder confidence(double confidence) {
            match.setConfidence(confidence);
            return this;
        }

        public Builder matched(boolean matched) {
            match.setMatched(matched);
            return this;
        }

        public UniversityMatch build() {
            return match;
        }
    }
}
