package com.bbmovie.droolsengine.dto;

import com.bbmovie.droolsengine.enums.VerificationOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Drools fact object for student verification scoring.
 * Populated by the OCR extraction service, then passed to the Drools engine.
 */
public class StudentVerificationContext {

    private UUID applicationId;
    private String userId;
    private String fullName;
    private String studentId;            // Extracted from OCR
    private String universityName;       // From application
    private String universityDomain;     // From application
    private String universityCountry;
    private Integer graduationYear;
    private String universityEmail;      // From application
    private String extractedText;        // Full OCR text
    private String extractedStudentId;   // Student ID found via OCR
    private String documentType;         // "pdf" or "image"
    private UniversityMatch universityMatch;

    // Scoring
    private int score = 0;
    private List<String> scoreReasons = new ArrayList<>();

    // Outcome (set after rules fire)
    private VerificationOutcome outcome;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getUniversityDomain() {
        return universityDomain;
    }

    public void setUniversityDomain(String universityDomain) {
        this.universityDomain = universityDomain;
    }

    public String getUniversityCountry() {
        return universityCountry;
    }

    public void setUniversityCountry(String universityCountry) {
        this.universityCountry = universityCountry;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getUniversityEmail() {
        return universityEmail;
    }

    public void setUniversityEmail(String universityEmail) {
        this.universityEmail = universityEmail;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getExtractedStudentId() {
        return extractedStudentId;
    }

    public void setExtractedStudentId(String extractedStudentId) {
        this.extractedStudentId = extractedStudentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public UniversityMatch getUniversityMatch() {
        return universityMatch;
    }

    public void setUniversityMatch(UniversityMatch universityMatch) {
        this.universityMatch = universityMatch;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getScoreReasons() {
        return scoreReasons;
    }

    public void setScoreReasons(List<String> scoreReasons) {
        this.scoreReasons = scoreReasons;
    }

    public VerificationOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(VerificationOutcome outcome) {
        this.outcome = outcome;
    }

    public void addScore(int points, String reason) {
        this.score += points;
        if (reason != null && !reason.isBlank()) {
            this.scoreReasons.add(reason);
        }
    }

    public VerificationOutcome determineOutcome(int autoApproveThreshold, int autoRejectThreshold) {
        if (this.score >= autoApproveThreshold) {
            this.outcome = VerificationOutcome.AUTO_APPROVE;
        } else if (this.score < autoRejectThreshold) {
            this.outcome = VerificationOutcome.AUTO_REJECT;
        } else {
            this.outcome = VerificationOutcome.NEEDS_REVIEW;
        }
        return this.outcome;
    }
}
