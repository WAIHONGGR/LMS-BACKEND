package com.tarumt.lms.service.user;

import com.tarumt.lms.dto.InstructorRequirementViewDTO;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.Instructor;
import com.tarumt.lms.model.InstructorQualification;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.QualificationStatus;
import com.tarumt.lms.repo.InstructorQualificationRepository;
import com.tarumt.lms.repo.InstructorRepository;
import com.tarumt.lms.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminInstructorQualificationService {

    @Autowired
    private InstructorQualificationRepository instructorQualificationRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private SupabaseStorageService storageService;

    @Value("${supabase.bucket.name}")
    private String bucketName;


    // ================================
    // GET All Instructor Requirements (Admin Table View - Basic Info Only)
    // ================================
    @Transactional(readOnly = true)
    public List<InstructorRequirementViewDTO> getAllInstructorRequirements(String status, String search) {
        List<InstructorQualification> qualifications;

        // Filter by status if provided (with instructor loaded)
        if (status != null && !status.trim().isEmpty()) {
            try {
                QualificationStatus statusEnum = QualificationStatus.valueOf(status.toUpperCase());
                qualifications = instructorQualificationRepository.findByStatusWithInstructor(statusEnum);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + status);
            }
        } else {
            // Load all with instructor
            qualifications = instructorQualificationRepository.findAllWithInstructor();
        }

        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            qualifications = qualifications.stream()
                    .filter(q -> {
                        Instructor instructor = q.getInstructor();
                        return instructor.getName().toLowerCase().contains(searchLower) ||
                                instructor.getEmail().toLowerCase().contains(searchLower);
                    })
                    .collect(Collectors.toList());
        }

        // Convert to DTO WITHOUT document/signature URLs (for table view - faster performance)
        return qualifications.stream()
                .map(this::convertToListViewDTO)
                .collect(Collectors.toList());
    }


    // ================================
    // GET Qualification Details (For View Details Modal - With Signed URLs)
    // ================================
    @Transactional(readOnly = true)
    public InstructorRequirementViewDTO getQualificationDetails(Long qualificationId) {
        log.info("Fetching qualification details: qualificationId={}", qualificationId);

        InstructorQualification qualification = instructorQualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new RuntimeException("Qualification not found"));

        Instructor instructor = qualification.getInstructor();
        if (instructor == null) {
            throw new RuntimeException("Instructor not found for qualification");
        }

        // Convert to DTO with signed URLs (convertToViewDTO already generates signed URLs)
        InstructorRequirementViewDTO dto = convertToViewDTO(qualification);

        log.info("Successfully fetched qualification details: qualificationId={}", qualificationId);
        return dto;
    }


    // ================================
    // UPDATE Qualification Status (Approve/Reject)
    // ================================
    @Transactional
    public void updateQualificationStatus(Long qualificationId, QualificationStatus newStatus, String rejectionReason, Admin actingAdmin) {
        InstructorQualification qual = instructorQualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new RuntimeException("Qualification not found"));

        // Validate status transition
        if (qual.getStatus() == QualificationStatus.REJECTED && newStatus == QualificationStatus.VERIFIED) {
            throw new RuntimeException("Cannot approve a rejected qualification");
        }

        if (actingAdmin == null) {
            log.error("Attempted to change qualification status without a valid admin. qualificationId={}", qualificationId);
            throw new IllegalArgumentException("Acting admin must not be null");
        }

        // If rejecting, delete file from Supabase and mark as DELETED
        if (newStatus == QualificationStatus.REJECTED && qual.getCertificateDocument() != null
                && !qual.getCertificateDocument().equals("DELETED")) {
            try {
                // Delete file from Supabase
                storageService.deleteFile(qual.getCertificateDocument(), bucketName);

                //reason for reject
                qual.setRejectionReason(rejectionReason);

                log.info("Successfully deleted and marked certificate document as DELETED for qualificationId={}", qualificationId);
            } catch (Exception e) {
                log.error("Error deleting file from Supabase for qualificationId={}: {}", qualificationId, e.getMessage(), e);
                // Mark as DELETED in DB even if physical deletion fails (for audit trail)
                qual.setCertificateDocument("DELETED");
                log.warn("Marked certificate document as DELETED in database despite deletion failure for qualificationId={}", qualificationId);
                // Continue with status update even if deletion fails
            }
        }

        // Note: Digital signature is stored in Instructor entity, not in qualification
        qual.setMadeByAdmin(actingAdmin);
        qual.setMadeAt(OffsetDateTime.now());
        qual.setMadeAt(OffsetDateTime.now());
        qual.setStatus(newStatus);
        instructorQualificationRepository.save(qual);

        log.info("Qualification id={} status updated to {}", qualificationId, newStatus);

        // Activate instructor if VERIFIED for the first time
        if (newStatus == QualificationStatus.VERIFIED) {
            Instructor instructor = qual.getInstructor(); // assuming @ManyToOne mapping
            if (instructor != null && instructor.getStatus() != AccountStatus.ACTIVE) {
                // Only activate if instructor is not already ACTIVE
                instructor.setStatus(AccountStatus.ACTIVE);
                instructor.setEndDate(null); // reactivate if soft-deleted
                instructorRepository.save(instructor);
                log.info("Instructor id={} status set to ACTIVE due to first qualification approval", instructor.getInstructorId());
            }
        }

    }

    // ================================
    // Helper Method: Convert to List View DTO (Table View - No URLs)
    // ================================
    private InstructorRequirementViewDTO convertToListViewDTO(InstructorQualification qualification) {
        Instructor instructor = qualification.getInstructor();

        InstructorRequirementViewDTO dto = new InstructorRequirementViewDTO();
        dto.setQualificationId(qualification.getId());
        dto.setInstructorId(instructor.getInstructorId());
        dto.setInstructorName(instructor.getName());
        dto.setInstructorEmail(instructor.getEmail());
        dto.setStatus(qualification.getStatus());
        dto.setQualificationLevel(qualification.getQualificationLevel());
        dto.setFieldOfStudy(qualification.getFieldOfStudy());
        dto.setRejectionReason(qualification.getRejectionReason());
        // DO NOT set documentUrl or digitalSignatureUrl for table view
        // These will be null/empty to improve performance
        // URLs are only generated when viewing details

        // Convert OffsetDateTime to LocalDateTime
        if (qualification.getSubmittedAt() != null) {
            dto.setSubmittedDate(qualification.getSubmittedAt().toLocalDateTime());
        }
        dto.setUpdatedDate(null);

        return dto;
    }


    // ================================
    // Helper Method: Convert to View DTO (Detail View - With Signed URLs)
    // ================================
    private InstructorRequirementViewDTO convertToViewDTO(InstructorQualification qualification) {
        Instructor instructor = qualification.getInstructor();

        InstructorRequirementViewDTO dto = new InstructorRequirementViewDTO();
        dto.setQualificationId(qualification.getId());
        dto.setInstructorId(instructor.getInstructorId());
        dto.setInstructorName(instructor.getName());
        dto.setInstructorEmail(instructor.getEmail());
        dto.setStatus(qualification.getStatus());
        dto.setQualificationLevel(qualification.getQualificationLevel());
        dto.setFieldOfStudy(qualification.getFieldOfStudy());
        dto.setRejectionReason(qualification.getRejectionReason());

        // Generate signed URLs for private bucket files
        if (qualification.getCertificateDocument() != null &&
                !qualification.getCertificateDocument().isBlank() &&
                !qualification.getCertificateDocument().equals("DELETED")) {
            try {
                String signedUrl = storageService.generateSignedUrl(
                        qualification.getCertificateDocument(),
                        bucketName,
                        3600 // 1 hour expiry
                );
                dto.setDocumentUrl(signedUrl);
                log.debug("Generated signed URL for certificate document: qualificationId={}", qualification.getId());
            } catch (Exception e) {
                log.error("Failed to generate signed URL for certificate document: {}",
                        qualification.getCertificateDocument(), e);
                dto.setDocumentUrl(qualification.getCertificateDocument()); // Fallback to original URL
            }
        }

        if (instructor.getDigitalSignature() != null && !instructor.getDigitalSignature().isBlank()) {
            try {
                String signedUrl = storageService.generateSignedUrl(
                        instructor.getDigitalSignature(),
                        bucketName,
                        3600 // 1 hour expiry
                );
                dto.setDigitalSignatureUrl(signedUrl);
                log.debug("Generated signed URL for digital signature: instructorId={}", instructor.getInstructorId());
            } catch (Exception e) {
                log.error("Failed to generate signed URL for digital signature: {}",
                        instructor.getDigitalSignature(), e);
                dto.setDigitalSignatureUrl(instructor.getDigitalSignature()); // Fallback to original URL
            }
        }

        // Convert OffsetDateTime to LocalDateTime
        if (qualification.getSubmittedAt() != null) {
            dto.setSubmittedDate(qualification.getSubmittedAt().toLocalDateTime());
        }
        dto.setUpdatedDate(null);

        return dto;
    }
}

