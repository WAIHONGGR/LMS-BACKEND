package com.tarumt.lms.service.user;

import com.tarumt.lms.dto.InstructorProfileUpdateDTO;
import com.tarumt.lms.dto.InstructorRequirementDTO;
import com.tarumt.lms.dto.InstructorRequirementStatusDTO;
import com.tarumt.lms.model.Instructor;
import com.tarumt.lms.model.InstructorQualification;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.QualificationStatus;
import com.tarumt.lms.repo.InstructorQualificationRepository;
import com.tarumt.lms.repo.InstructorRepository;
import com.tarumt.lms.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class InstructorService {

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private InstructorQualificationRepository instructorQualificationRepository;

    @Autowired
    private SupabaseStorageService storageService;


    // ================================
    // Basic CRUD Operations
    // ================================
    public boolean existsByEmail(String email) {
        return instructorRepository.existsByEmail(email);
    }

    public Optional<Instructor> getByUserId(UUID userId) {
        log.info("Fetching instructor profile for userId={}", userId);
        return instructorRepository.findByUserId(userId)
                .map(instructor -> {
                    log.info("Instructor found: id={}, userId={}", instructor.getUserId(), instructor.getInstructorId());
                    return instructor;
                })
                .or(() -> {
                    log.warn("Instructor not found for userId={}", userId);
                    return Optional.empty();
                });
    }

    public Optional<Instructor> getById(Long instructorId) {
        log.info("Fetching instructor profile for instructor={}", instructorId);
        return instructorRepository.findByInstructorId(instructorId)
                .map(instructor -> {
                    log.info("log found instructorId={}", instructorId);
                    return instructor;
                })
                .or(() -> {
                    log.warn("instructor not found for instructorId={}", instructorId);
                    return Optional.empty();
                });
    }

    public Optional<Instructor> getByEmail(String email) {
        log.info("Fetching instructor profile for email={}", email);
        return instructorRepository.findByEmail(email)
                .map(instructor -> {
                    log.info("Instructor found: id={}, email={}", instructor.getInstructorId(), instructor.getEmail());
                    return instructor;
                })
                .or(() -> {
                    log.warn("Instructor not found for email={}", email);
                    return Optional.empty();
                });
    }

    @Transactional
    public Instructor create(String email, String name, UUID supabaseUserId) {
        log.info("Creating new instructor: email={}, supabaseUserId={}", email, supabaseUserId);
        Instructor instructor = new Instructor();
        instructor.setUserId(supabaseUserId);
        instructor.setEmail(email);
        instructor.setName(name);
        Instructor savedInstructor = instructorRepository.save(instructor);
        log.info("Instructor created successfully: id={}", savedInstructor.getInstructorId());
        return savedInstructor;
    }


    // ================================
    // Basic CRUD Operations (with filter ensure is active user)
    // ================================

    public Optional<Instructor> getActiveOrPendingByEmail(String email) {
        log.info("Fetching instructor by email={} excluding INACTIVE", email);
        return instructorRepository.findByEmailAndStatusNot(email, AccountStatus.INACTIVE)
                .map(instructor -> {
                    log.info("Instructor found by email: {} with status={}", instructor.getEmail(), instructor.getStatus());
                    return instructor;
                })
                .or(() -> {
                    log.warn("No active or pending instructor found for email={}", email);
                    return Optional.empty();
                });
    }

    public Optional<Instructor> getActiveOrPendingByUserId(UUID userId) {
        log.info("Fetching instructor by userId={} excluding INACTIVE", userId);
        return instructorRepository.findByUserIdAndStatusNot(userId, AccountStatus.INACTIVE)
                .map(instructor -> {
                    log.info("Instructor found by userId: {} with status={}", instructor.getUserId(), instructor.getStatus());
                    return instructor;
                })
                .or(() -> {
                    log.warn("No active or pending instructor found for userId={}", userId);
                    return Optional.empty();
                });
    }

    public Optional<Instructor> getActiveOrPendingById(Long instructorId) {
        log.info("Fetching instructor profile for instructorId={} (excluding INACTIVE)", instructorId);
        return instructorRepository.findByInstructorIdAndStatusNot(instructorId, AccountStatus.INACTIVE)
                .map(instructor -> {
                    log.info("Instructor found: instructorId={}, email={}, status={}",
                            instructor.getInstructorId(), instructor.getEmail(), instructor.getStatus());
                    return instructor;
                })
                .or(() -> {
                    log.warn("Instructor not found or inactive for instructorId={}", instructorId);
                    return Optional.empty();
                });
    }


    // ================================
    // Profile Management (Instructor Self-Service)
    // ================================
    @Transactional
    public Instructor updateInstructorProfile(Long instructorId, InstructorProfileUpdateDTO dto) {
        log.info("Updating instructor profile for instructorId={}, updateData={}", instructorId, dto);

        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> {
                    log.warn("Instructor not found with ID {}", instructorId);
                    return new RuntimeException("Instructor not found with ID " + instructorId);
                });

        instructor.setName(dto.getName().trim());
        instructor.setPhoneNum(dto.getPhoneNum() != null && dto.getPhoneNum().isBlank() ? null : dto.getPhoneNum());

        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date parsedDate = sdf.parse(dto.getBirthDate());
                if (parsedDate.after(new Date())) {
                    log.warn("Birth date cannot be in the future: {}", dto.getBirthDate());
                    throw new RuntimeException("Birth date cannot be in the future");
                }
                instructor.setBirthDate(parsedDate);
            } catch (ParseException e) {
                log.error("Invalid birth date format: {}", dto.getBirthDate(), e);
                throw new RuntimeException("Invalid birth date format: " + dto.getBirthDate());
            }
        } else {
            instructor.setBirthDate(null);
        }

        Instructor saveInstructor = instructorRepository.save(instructor);
        log.info("Instructor profile updated successfully: instructorId={}", saveInstructor.getInstructorId());
        return saveInstructor;
    }


    // ================================
    // Requirements Management (Instructor Self-Service)
    // ================================

    // Retrieve Requirements Info
    public InstructorRequirementStatusDTO getRequirementsStatus(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        List<InstructorQualification> qualifications =
                instructorQualificationRepository.findByInstructor_InstructorIdOrderBySubmittedAtDesc(instructorId);

        boolean hasDigitalSignature = instructor.getDigitalSignature() != null;
        boolean hasCertificate = !qualifications.isEmpty();
        boolean latestCertificatePending = hasCertificate && qualifications.get(0).isPending();
        boolean canSubmit = hasDigitalSignature && !latestCertificatePending;

        InstructorRequirementStatusDTO statusDTO = new InstructorRequirementStatusDTO();
        statusDTO.setHasDigitalSignature(hasDigitalSignature);
        statusDTO.setHasCertificate(hasCertificate);
        statusDTO.setLatestCertificatePending(latestCertificatePending);
        statusDTO.setCanSubmit(canSubmit);

        return statusDTO;
    }


    public List<InstructorQualification> getSubmissionHistory(Long instructorId) {
        log.info("Fetching submission history for instructorId={}", instructorId);

        // Verify instructor exists
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found with ID: " + instructorId));

        // Get all qualifications for this instructor, ordered by submission date (newest first)
        List<InstructorQualification> qualifications = instructorQualificationRepository
                .findByInstructorOrderBySubmittedAtDesc(instructor);

        log.info("Found {} qualification submissions for instructorId={}", qualifications.size(), instructorId);
        return qualifications;
    }


    // submitRequirements method
    @Transactional
    public Instructor submitRequirements(Long instructorId, InstructorRequirementDTO dto) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        boolean firstSubmission = instructor.getDigitalSignature() == null;

        // --- First submission: digital signature required ---
        if (firstSubmission) {
            if (dto.getDigitalSignatureBase64() == null || dto.getDigitalSignatureBase64().isBlank()) {
                throw new IllegalArgumentException("Digital signature is required for first submission");
            }

            String signatureUrl = storageService.uploadBase64(
                    dto.getDigitalSignatureBase64(),
                    "Instructor-Docs",
                    "digital-signature",
                    "instructor_" + instructorId + ".png",
                    "image/png"
            );
            instructor.setDigitalSignature(signatureUrl);
        } else {
            if (dto.getDigitalSignatureBase64() != null) {
                log.info("Digital signature ignored for instructorId={} because it was already submitted", instructorId);
            }
        }

        // --- Qualification documents ---
        if (dto.getQualificationFiles() != null && !dto.getQualificationFiles().isEmpty()) {
            List<MultipartFile> files = dto.getQualificationFiles();
            List<String> levels = dto.getQualificationLevels() != null ? dto.getQualificationLevels() : new ArrayList<>();
            List<String> fieldsOfStudy = dto.getFieldOfStudy() != null ? dto.getFieldOfStudy() : new ArrayList<>();

            // Validate that arrays have matching sizes (if provided)
            if (!levels.isEmpty() && levels.size() != files.size()) {
                throw new IllegalArgumentException("Number of qualification levels must match number of files");
            }
            if (!fieldsOfStudy.isEmpty() && fieldsOfStudy.size() != files.size()) {
                throw new IllegalArgumentException("Number of fields of study must match number of files");
            }

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                if (!"application/pdf".equals(file.getContentType())) {
                    throw new RuntimeException("Only PDF files are allowed for certificates");
                }
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new RuntimeException("File size exceeds 10 MB");
                }

                String fileUrl = storageService.uploadFile(
                        file,
                        "Instructor-Docs",
                        "certificates",
                        "instructor_" + instructorId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename()
                );

                // Map DTO info to entity - FIXED: Use index to match file with its level and fieldOfStudy
                InstructorQualification qual = new InstructorQualification();
                qual.setInstructor(instructor);
                qual.setCertificateDocument(fileUrl);
                qual.setStatus(QualificationStatus.PENDING);

                // Use index to get corresponding level and fieldOfStudy
                if (i < levels.size() && levels.get(i) != null && !levels.get(i).isBlank()) {
                    qual.setQualificationLevel(levels.get(i));
                } else {
                    qual.setQualificationLevel("CERTIFICATE"); // default if missing
                }

                if (i < fieldsOfStudy.size() && fieldsOfStudy.get(i) != null && !fieldsOfStudy.get(i).isBlank()) {
                    qual.setFieldOfStudy(fieldsOfStudy.get(i));
                }

                instructorQualificationRepository.save(qual);
            }
        } else if (firstSubmission) {
            throw new IllegalArgumentException("At least one qualification document is required for first submission");
        }

        return instructorRepository.save(instructor);
    }



}