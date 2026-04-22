package com.bbmovie.droolsengine.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversityMatchTest {

    @Test
    void builderAndSettersWork() {
        UniversityMatch built = UniversityMatch.builder()
                .name("Test University")
                .domain("test.edu")
                .country("VN")
                .confidence(0.8)
                .matched(true)
                .build();

        assertEquals("Test University", built.getName());
        assertEquals("test.edu", built.getDomain());
        assertEquals("VN", built.getCountry());
        assertEquals(0.8, built.getConfidence());
        assertTrue(built.isMatched());

        UniversityMatch plain = new UniversityMatch();
        plain.setName("Another");
        plain.setDomain("another.edu");
        plain.setCountry("US");
        plain.setConfidence(0.2);
        plain.setMatched(false);

        assertEquals("Another", plain.getName());
        assertEquals("another.edu", plain.getDomain());
        assertEquals("US", plain.getCountry());
        assertEquals(0.2, plain.getConfidence());
        assertFalse(plain.isMatched());
    }
}
