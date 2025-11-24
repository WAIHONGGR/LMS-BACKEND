package com.tarumt.lms.service.user;

import com.tarumt.lms.model.SuperAdmin;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.repo.SuperAdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SuperAdminService {

    @Autowired
    private SuperAdminRepository superAdminRepository;

    // ================================
    // Basic CRUD / Fetch Operations
    // ================================

    public Optional<SuperAdmin> getByEmail(String email) {
        log.info("Fetching SuperAdmin by email={}", email);
        return superAdminRepository.findByEmail(email)
                .map(sa -> {
                    log.info("SuperAdmin found: superAdminId={}, email={}", sa.getSuperAdminId(), sa.getEmail());
                    return sa;
                })
                .or(() -> {
                    log.warn("SuperAdmin not found for email={}", email);
                    return Optional.empty();
                });
    }

    public Optional<SuperAdmin> getByUserId(UUID userId) {
        log.info("Fetching SuperAdmin by userId={}", userId);
        return superAdminRepository.findByUserId(userId)
                .map(sa -> {
                    log.info("SuperAdmin found: superAdminId={}, userId={}", sa.getSuperAdminId(), sa.getUserId());
                    return sa;
                })
                .or(() -> {
                    log.warn("SuperAdmin not found for userId={}", userId);
                    return Optional.empty();
                });
    }

    public Optional<SuperAdmin> getActiveByEmail(String email) {
        log.info("Fetching active SuperAdmin by email={}", email);
        return superAdminRepository.findByEmailAndStatus(email, AccountStatus.ACTIVE)
                .map(sa -> {
                    log.info("Active SuperAdmin found: superAdminId={}, email={}", sa.getSuperAdminId(), sa.getEmail());
                    return sa;
                })
                .or(() -> {
                    log.warn("No active SuperAdmin found for email={}", email);
                    return Optional.empty();
                });
    }

    public Optional<SuperAdmin> getActiveByUserId(UUID userId) {
        log.info("Fetching active SuperAdmin by userId={}", userId);
        return superAdminRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
                .map(sa -> {
                    log.info("Active SuperAdmin found: superAdminId={}, userId={}", sa.getSuperAdminId(), sa.getUserId());
                    return sa;
                })
                .or(() -> {
                    log.warn("No active SuperAdmin found for userId={}", userId);
                    return Optional.empty();
                });
    }

    public boolean existsByEmail(String email) {
        return superAdminRepository.existsByEmail(email);
    }



}
