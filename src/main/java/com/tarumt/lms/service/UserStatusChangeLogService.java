package com.tarumt.lms.service;

import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.UserStatusChangeLog;
import com.tarumt.lms.model.enums.Role;
import com.tarumt.lms.repo.UserStatusChangeLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class UserStatusChangeLogService {

    private final UserStatusChangeLogRepository logRepository;

    public UserStatusChangeLogService(UserStatusChangeLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public void logStatusChange(Role userType, Long userId, String oldStatus, String newStatus, Admin admin, String reason) {
        if (userType == null || userId == null || admin == null) {
            throw new IllegalArgumentException("UserType, userId, and admin must not be null");
        }

        try {
            UserStatusChangeLog logEntry = UserStatusChangeLog.builder()
                    .userType(userType)
                    .userId(userId)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .changedByAdmin(admin)
                    .changedAt(OffsetDateTime.now())
                    .reason(reason)
                    .build();

            logRepository.save(logEntry);
        } catch (Exception e) {
            // Log the failure
            log.error("Failed to save user status change log for userId={}: {}", userId, e.getMessage(), e);
            throw e; // optionally rethrow if you want transaction rollback
        }
    }

    /**
     * Get all status change logs for a specific user type (STUDENT or INSTRUCTOR)
     * @param userType The user type as string (e.g., "STUDENT", "INSTRUCTOR")
     * @return List of status change logs, ordered by most recent first
     */
    public List<UserStatusChangeLog> getLogsByUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            throw new IllegalArgumentException("UserType must not be null or blank");
        }
        // Convert String to Role enum
        Role role;
        try {
            role = Role.valueOf(userType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid userType. Must be STUDENT or INSTRUCTOR");
        }
        return logRepository.findByUserTypeOrderByChangedAtDesc(role);
    }

    /**
     * Get all status change logs for a specific user ID
     * @param userId The user ID
     * @return List of status change logs, ordered by most recent first
     */
    public List<UserStatusChangeLog> getLogsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId must not be null");
        }
        return logRepository.findByUserIdOrderByChangedAtDesc(userId);
    }

    /**
     * Get all status change logs for a specific user type and user ID
     * @param userType The user type as string (e.g., "STUDENT", "INSTRUCTOR")
     * @param userId The user ID
     * @return List of status change logs, ordered by most recent first
     */
    public List<UserStatusChangeLog> getLogsByUserTypeAndUserId(String userType, Long userId) {
        if (userType == null || userType.isBlank()) {
            throw new IllegalArgumentException("UserType must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId must not be null");
        }
        // Convert String to Role enum
        Role role;
        try {
            role = Role.valueOf(userType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid userType. Must be STUDENT or INSTRUCTOR");
        }
        return logRepository.findByUserTypeAndUserIdOrderByChangedAtDesc(role, userId);
    }
}

