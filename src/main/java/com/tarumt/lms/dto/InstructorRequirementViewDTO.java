package com.tarumt.lms.dto;

import com.tarumt.lms.model.enums.QualificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorRequirementViewDTO {
    private Long qualificationId;
    private Long instructorId;
    private String instructorName;
    private String instructorEmail;
    private QualificationStatus status;
    private String qualificationLevel; // e.g., "DEGREE", "CERTIFICATE", etc.
    private String documentUrl; // URL to the uploaded document
    private String digitalSignatureUrl; // URL to the digital signature
    private LocalDateTime submittedDate;
    private LocalDateTime updatedDate;
    private String fieldOfStudy;
    private String rejectionReason;
}