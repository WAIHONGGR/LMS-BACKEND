package com.tarumt.lms.service.user;

import com.tarumt.lms.dto.InstructorDetailViewDTO;
import com.tarumt.lms.dto.InstructorListViewDTO;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.Instructor;
import com.tarumt.lms.model.InstructorQualification;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.QualificationStatus;
import com.tarumt.lms.model.enums.Role;
import com.tarumt.lms.repo.InstructorQualificationRepository;
import com.tarumt.lms.repo.InstructorRepository;
import com.tarumt.lms.repo.UserStatusChangeLogRepository;
import com.tarumt.lms.service.SupabaseStorageService;
import com.tarumt.lms.service.UserStatusChangeLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminInstructorManagementService {

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private InstructorQualificationRepository instructorQualificationRepository;

    @Autowired
    private SupabaseStorageService storageService;

    @Autowired
    private UserStatusChangeLogService userStatusChangeLogService;

    @Value("${supabase.bucket.name}")
    private String bucketName;

    // ================================
    // GET All Instructors (Basic Info Only - For Table View)
    // ================================
    @Transactional(readOnly = true)
    public List<InstructorListViewDTO> getAllInstructors(String status, String search) {
        log.info("Fetching all instructors (basic info): status={}, search={}", status, search);

        List<Instructor> instructors;

        // Filter by status if provided and not "all"
        AccountStatus filterStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                filterStatus = AccountStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter provided: {}. Returning empty list.", status);
                return Collections.emptyList();
            }
        }

        // Fetch instructors based on status filter
        if (filterStatus != null) {
            instructors = instructorRepository.findByStatus(filterStatus);
        } else {
            instructors = instructorRepository.findAll();
        }

        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            instructors = instructors.stream()
                    .filter(i ->
                            (i.getName() != null && i.getName().toLowerCase().contains(searchLower)) ||
                                    (i.getEmail() != null && i.getEmail().toLowerCase().contains(searchLower))
                    )
                    .collect(Collectors.toList());
        }

        // Convert to basic DTO (ONLY 5 fields: instructorId, name, email, status, registeredDate)
        return instructors.stream()
                .map(this::convertToListViewDTO)
                .collect(Collectors.toList());
    }


    // ================================
    // GET Instructor Details with Certificates (Admin View - For View Details Modal)
    // ================================
    @Transactional(readOnly = true)
    public InstructorDetailViewDTO getInstructorDetailsForAdmin(Long instructorId) {
        log.info("Fetching instructor details for admin: instructorId={}", instructorId);

        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        // Fetch all qualifications for this instructor
        List<InstructorQualification> qualifications =
                instructorQualificationRepository.findByInstructor_InstructorIdOrderBySubmittedAtDesc(instructorId);

        // Build DTO
        InstructorDetailViewDTO dto = new InstructorDetailViewDTO();
        dto.setInstructorId(instructor.getInstructorId());
        dto.setUserId(instructor.getUserId() != null ? instructor.getUserId().toString() : null);
        dto.setEmail(instructor.getEmail());
        dto.setName(instructor.getName());
        dto.setPhoneNum(instructor.getPhoneNum());
        dto.setStatus(instructor.getStatus().name());
        dto.setBirthDate(instructor.getBirthDate() != null ?
                new SimpleDateFormat("yyyy-MM-dd").format(instructor.getBirthDate()) : null);
        dto.setRegisteredDate(instructor.getRegisteredDate());
        dto.setEndDate(instructor.getEndDate());

        // Generate signed URL for digital signature
        if (instructor.getDigitalSignature() != null && !instructor.getDigitalSignature().isBlank()) {
            try {
                String signedUrl = storageService.generateSignedUrl(
                        instructor.getDigitalSignature(),
                        bucketName,
                        3600 // 1 hour expiry
                );
                dto.setDigitalSignatureUrl(signedUrl);
                log.debug("Generated signed URL for digital signature: instructorId={}", instructorId);
            } catch (Exception e) {
                log.error("Failed to generate signed URL for digital signature: {}",
                        instructor.getDigitalSignature(), e);
                dto.setDigitalSignatureUrl(instructor.getDigitalSignature()); // Fallback
            }
        }

        // Convert qualifications to DTOs with signed URLs
        // FILTER: Only show VERIFIED certificates
        List<InstructorDetailViewDTO.CertificateInfoDTO> certificateDTOs = qualifications.stream()
                .filter(qual -> qual.getStatus() == QualificationStatus.VERIFIED) // Only VERIFIED certificates
                .map(qual -> {
                    InstructorDetailViewDTO.CertificateInfoDTO certDTO =
                            new InstructorDetailViewDTO.CertificateInfoDTO();
                    certDTO.setQualificationId(qual.getId());
                    certDTO.setQualificationLevel(qual.getQualificationLevel());
                    certDTO.setStatus(qual.getStatus());
                    certDTO.setSubmittedDate(qual.getSubmittedAt());
                    certDTO.setFieldOfStudy(qual.getFieldOfStudy());
                    certDTO.setRejectionReason(qual.getRejectionReason());

                    // Generate signed URL for certificate document
                    if (qual.getCertificateDocument() != null &&
                            !qual.getCertificateDocument().isBlank() &&
                            !qual.getCertificateDocument().equals("DELETED")) {
                        try {
                            String signedUrl = storageService.generateSignedUrl(
                                    qual.getCertificateDocument(),
                                    bucketName,
                                    3600 // 1 hour expiry
                            );
                            certDTO.setDocumentUrl(signedUrl);
                            log.debug("Generated signed URL for certificate: qualificationId={}", qual.getId());
                        } catch (Exception e) {
                            log.error("Failed to generate signed URL for certificate: {}",
                                    qual.getCertificateDocument(), e);
                            certDTO.setDocumentUrl(qual.getCertificateDocument()); // Fallback
                        }
                    }

                    return certDTO;
                })
                .collect(Collectors.toList());

        dto.setCertificates(certificateDTOs);

        log.info("Successfully fetched instructor details: instructorId={}, verifiedCertificatesCount={}",
                instructorId, certificateDTOs.size());
        return dto;
    }


    // ================================
    // UPDATE Instructor Status (Activate/Deactivate - Soft Delete)
    // ================================
    @Transactional
    public void updateInstructorStatus(Long instructorId, AccountStatus status, Admin admin, String reason) {

        log.info("Updating instructor status: instructorId={}, newStatus={}", instructorId, status);

        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        AccountStatus oldStatus = instructor.getStatus();

        // Update status
        instructor.setStatus(status);

        // handle soft-delete behavior
        if (status == AccountStatus.INACTIVE) {
            instructor.setEndDate(OffsetDateTime.now());
            log.info("Instructor deactivated (soft delete): instructorId={}, endDate={}",
                    instructorId, instructor.getEndDate());
        } else if (status == AccountStatus.ACTIVE) {
            instructor.setEndDate(null); // reactivate
            log.info("Instructor reactivated: instructorId={}", instructorId);
        }

        instructorRepository.save(instructor);

        // Log status change
        userStatusChangeLogService.logStatusChange(
                Role.INSTRUCTOR,
                instructorId,
                oldStatus.name(),
                status.name(),
                admin,
                reason
        );

        log.info("Successfully updated instructor status and logged change: instructorId={}, newStatus={}",
                instructorId, status);
    }



    private boolean isValidStatus(String status) {
        if (status == null) return false;

        try {
            AccountStatus.valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }



    // ================================
    // Helper Method: Convert to List View DTO (Basic Info Only - 5 Fields)
    // ================================
    private InstructorListViewDTO convertToListViewDTO(Instructor instructor) {
        InstructorListViewDTO dto = new InstructorListViewDTO();
        dto.setInstructorId(instructor.getInstructorId());
        dto.setName(instructor.getName());
        dto.setEmail(instructor.getEmail());
        dto.setStatus(instructor.getStatus().name());
        dto.setRegisteredDate(instructor.getRegisteredDate());
        // Only these 5 fields - removed phoneNum and endDate
        return dto;
    }



}