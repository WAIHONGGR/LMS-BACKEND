package com.tarumt.lms.dto;

import com.tarumt.lms.model.enums.QualificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorDetailViewDTO {
    private Long instructorId;
    private String userId;
    private String email;
    private String name;
    private String phoneNum;
    private String status;
    private String birthDate;
    private OffsetDateTime registeredDate;
    private OffsetDateTime endDate;

    // Digital signature with signed URL
    private String digitalSignatureUrl;

    // List of certificates/qualifications
    private List<CertificateInfoDTO> certificates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateInfoDTO {
        private Long qualificationId;
        private String qualificationLevel;
        private QualificationStatus status;
        private String documentUrl; // Signed URL
        private OffsetDateTime submittedDate;
        private String fieldOfStudy;
        private String rejectionReason;
    }
}