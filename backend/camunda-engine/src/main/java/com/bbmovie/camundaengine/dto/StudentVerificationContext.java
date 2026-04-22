package com.bbmovie.camundaengine.dto;

import com.bbmovie.camundaengine.enums.VerificationOutcome;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class StudentVerificationContext {

    private UUID applicationId;
    private String userId;
    private String fullName;
    private String studentId;
    private String universityName;
    private String universityDomain;
    private String universityCountry;
    private Integer graduationYear;
    private String universityEmail;
    private String extractedText;
    private String extractedStudentId;
    private String documentType;
    private UniversityMatch universityMatch;

    private int score = 0;
    private List<String> scoreReasons = new ArrayList<>();

    private VerificationOutcome outcome;
}
