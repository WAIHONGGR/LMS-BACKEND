package com.tarumt.lms.service.user;

import com.tarumt.lms.dto.AdminProfileUpdateDTO;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.AdminStatusChangeLog;
import com.tarumt.lms.model.SuperAdmin;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.Role;
import com.tarumt.lms.repo.AdminRepository;
import com.tarumt.lms.repo.AdminStatusChangeLogRepository;
import com.tarumt.lms.repo.UserRoleRepository;
import com.tarumt.lms.service.AdminStatusChangeLogService;
import com.tarumt.lms.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private AdminStatusChangeLogService adminStatusChangeLogService;


    // ================================
    // Basic CRUD Operations
    // ================================

    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
    }

    public Optional<Admin> getById(Long adminId) {
        log.info("Fetching admin profile for adminId={}", adminId);
        return adminRepository.findByAdminId(adminId)
                .map(admin -> {
                    log.info("Admin found: adminId={}, email={}", admin.getAdminId(), admin.getEmail());
                    return admin;
                })
                .or(() -> {
                    log.warn("Admin not found for adminId={}", adminId);
                    return Optional.empty();
                });
    }

    public Optional<Admin> getByUserId(UUID userId) {
        log.info("Fetching admin profile for userId={}", userId);
        return adminRepository.findByUserId(userId)
                .map(admin -> {
                    log.info("Admin found: adminId={}, userId={}", admin.getAdminId(), admin.getUserId());
                    return admin;
                })
                .or(() -> {
                    log.warn("Admin not found for userId={}", userId);
                    return Optional.empty();
                });
    }


    public Optional<Admin> getByEmail(String email) {
        log.info("Fetching admin profile for email={}", email);
        return adminRepository.findByEmail(email)
                .map(admin -> {
                    log.info("Admin found: adminId={}, email={}", admin.getAdminId(), admin.getEmail());
                    return admin;
                })
                .or(() -> {
                    log.warn("Admin not found for email={}", email);
                    return Optional.empty();
                });
    }

    public Optional<Admin> getActiveByEmail(String email) {
        log.info("Fetching active admin profile for email={}", email);
        return adminRepository.findByEmailAndStatus(email, AccountStatus.ACTIVE)
                .map(admin -> {
                    log.info("Active admin found: adminId={}, email={}", admin.getAdminId(), admin.getEmail());
                    return admin;
                })
                .or(() -> {
                    log.warn("Active admin not found for email={}", email);
                    return Optional.empty();
                });
    }

    public Optional<Admin> getActiveAdminById(Long adminId) {
        log.info("Fetching active admin profile for adminId={}", adminId);
        return adminRepository.findByAdminIdAndStatus(adminId, AccountStatus.ACTIVE)
                .map(admin -> {
                    log.info("Active admin found: adminId={}, email={}", admin.getAdminId(), admin.getEmail());
                    return admin;
                })
                .or(() -> {
                    log.warn("Active admin not found for adminId={}", adminId);
                    return Optional.empty();
                });
    }

    public Optional<Admin> getActiveByUserId(UUID userId) {
        log.info("Fetching ACTIVE admin by userId={}", userId);
        return adminRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
                .map(admin -> {
                    log.info("Active admin found: adminId={}, userId={}", admin.getAdminId(), admin.getUserId());
                    return admin;
                })
                .or(() -> {
                    log.warn("No active admin found for userId={}", userId);
                    return Optional.empty();
                });
    }

    public List<Admin> getByStatus(AccountStatus status) {
        log.info("Fetching admins with status={}", status);

        List<Admin> admins = adminRepository.findByStatus(status);

        if (admins.isEmpty()) {
            log.warn("No admins found with status={}", status);
        } else {
            log.info("Found {} admins with status={}", admins.size(), status);
        }

        return admins;
    }

    @Transactional(readOnly = true)
    public List<Admin> getAllAdmins() {
        log.info("Fetching all admins");

        List<Admin> admins = adminRepository.findAll();

        if (admins.isEmpty()) {
            log.warn("No admins found in the system");
        } else {
            log.info("Found {} admins", admins.size());
        }

        return admins;
    }


    @Transactional
    public Admin save(Admin admin) {
        if (admin == null) {
            log.warn("Attempted to save null admin object");
            throw new IllegalArgumentException("Admin cannot be null");
        }

        log.info("Saving admin: adminId={}, email={}", admin.getAdminId(), admin.getEmail());
        Admin saved = adminRepository.save(admin);
        log.info("Admin saved successfully: adminId={}, email={}", saved.getAdminId(), saved.getEmail());
        return saved;
    }


    @Transactional
    public Admin create(String email, String name, UUID supabaseUserId) {
        log.info("Creating new admin: email={}, supabaseUserId={}", email, supabaseUserId);

        Admin admin = Admin.builder()
                .email(email)
                .name(name)
                .status(AccountStatus.ACTIVE)
                .registeredDate(LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC))
                .build();

        // Only set userId if provided (not null)
        // If null, it will be set when the admin logs in with Google/Supabase
        if (supabaseUserId != null) {
            admin.setUserId(supabaseUserId);
        }

        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin created successfully: adminId={}, userId={}", savedAdmin.getAdminId(), savedAdmin.getUserId());
        return savedAdmin;
    }

    @Transactional
    public Admin handleAdminLogin(String email, UUID userId) {
        Optional<Admin> existingByEmail = adminRepository.findByEmail(email);
        Optional<Admin> existingByUserId = adminRepository.findByUserId(userId);

        Admin admin;

        if (existingByEmail.isPresent()) {
            admin = existingByEmail.get();

            if (admin.getUserId() == null) {
                admin.setUserId(userId);
                admin = adminRepository.save(admin);

                // Use UserRoleService instead of repository
                userRoleService.createUserRole(userId, Role.ADMIN);

                log.info("Updated admin userId from null to: {}", userId);
            }
        } else if (existingByUserId.isPresent()) {
            admin = existingByUserId.get();
        } else {
            throw new IllegalStateException("Admin does not exist for login");
        }

        if (admin.getStatus() == AccountStatus.INACTIVE) {
            throw new IllegalStateException("Admin account is inactive");
        }

        return admin;
    }

    @Transactional
    public Admin updateAdminProfile(Long adminId, AdminProfileUpdateDTO dto) {
        Admin admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        admin.setName(dto.getName().trim());
        admin.setPhoneNum(dto.getPhoneNum() != null && dto.getPhoneNum().isBlank() ? null : dto.getPhoneNum());

        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date parsedDate = sdf.parse(dto.getBirthDate());

                if (parsedDate.after(new Date())) {
                    throw new RuntimeException("Birth date cannot be in the future");
                }

                admin.setBirthDate(parsedDate);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid birth date format: " + dto.getBirthDate());
            }
        } else {
            admin.setBirthDate(null);
        }

        return adminRepository.save(admin);
    }


    // =====================================================
    // UPDATE ADMIN STATUS (ACTIVE / INACTIVE)
    // =====================================================
    @Transactional
    public Admin updateAdminStatus(Long adminId, AccountStatus newStatus, SuperAdmin superAdmin, String reason) {
        log.info("Updating admin status: id={}, newStatus={}", adminId, newStatus);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        AccountStatus oldStatus = admin.getStatus();

        // Update admin status
        admin.setStatus(newStatus);
        admin.setEndDate(newStatus == AccountStatus.INACTIVE ? OffsetDateTime.now() : null);

        Admin updated = adminRepository.save(admin);

        // Log via separate service
        adminStatusChangeLogService.logStatusChange(updated, oldStatus, newStatus, superAdmin, reason);

        log.info("Updated admin status successfully: id={}, oldStatus={}, newStatus={}, reason={}, endDate={}",
                adminId, oldStatus, newStatus, reason, updated.getEndDate());

        return updated;
    }



}
