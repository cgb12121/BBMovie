package com.bbmovie.droolsengine.dto;

import com.bbmovie.droolsengine.enums.VerificationOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StudentVerificationContextTest {

    @Test
    void addScoreAccumulatesPointsAndReason() {
        StudentVerificationContext context = new StudentVerificationContext();

        context.addScore(10, "rule 1");
        context.addScore(5, "rule 2");
        context.addScore(1, "  ");

        assertEquals(16, context.getScore());
        assertEquals(2, context.getScoreReasons().size());
    }

    @Test
    void determineOutcomeReturnsExpectedStates() {
        StudentVerificationContext context = new StudentVerificationContext();

        context.setScore(85);
        assertEquals(VerificationOutcome.AUTO_APPROVE, context.determineOutcome(80, 30));

        context.setScore(20);
        assertEquals(VerificationOutcome.AUTO_REJECT, context.determineOutcome(80, 30));

        context.setScore(50);
        assertEquals(VerificationOutcome.NEEDS_REVIEW, context.determineOutcome(80, 30));
    }

    @Test
    void gettersAndSettersWorkForAllFields() {
        StudentVerificationContext context = new StudentVerificationContext();
        UUID appId = UUID.randomUUID();
        UniversityMatch match = UniversityMatch.builder().name("Uni").domain("uni.edu").country("VN").confidence(0.9).matched(true).build();
        List<String> reasons = List.of("r1", "r2");

        context.setApplicationId(appId);
        context.setUserId("u1");
        context.setFullName("Full Name");
        context.setStudentId("S123");
        context.setUniversityName("Uni");
        context.setUniversityDomain("uni.edu");
        context.setUniversityCountry("VN");
        context.setGraduationYear(2028);
        context.setUniversityEmail("student@uni.edu");
        context.setExtractedText("OCR");
        context.setExtractedStudentId("S123");
        context.setDocumentType("pdf");
        context.setUniversityMatch(match);
        context.setScore(70);
        context.setScoreReasons(reasons);
        context.setOutcome(VerificationOutcome.NEEDS_REVIEW);

        assertEquals(appId, context.getApplicationId());
        assertEquals("u1", context.getUserId());
        assertEquals("Full Name", context.getFullName());
        assertEquals("S123", context.getStudentId());
        assertEquals("Uni", context.getUniversityName());
        assertEquals("uni.edu", context.getUniversityDomain());
        assertEquals("VN", context.getUniversityCountry());
        assertEquals(2028, context.getGraduationYear());
        assertEquals("student@uni.edu", context.getUniversityEmail());
        assertEquals("OCR", context.getExtractedText());
        assertEquals("S123", context.getExtractedStudentId());
        assertEquals("pdf", context.getDocumentType());
        assertSame(match, context.getUniversityMatch());
        assertEquals(70, context.getScore());
        assertEquals(reasons, context.getScoreReasons());
        assertEquals(VerificationOutcome.NEEDS_REVIEW, context.getOutcome());
    }
}
