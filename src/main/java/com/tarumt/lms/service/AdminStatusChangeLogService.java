package com.tarumt.lms.service;

import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.AdminStatusChangeLog;
import com.tarumt.lms.model.SuperAdmin;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.repo.AdminStatusChangeLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class AdminStatusChangeLogService {

    private final AdminStatusChangeLogRepository logRepository;

    public AdminStatusChangeLogService(AdminStatusChangeLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public void logStatusChange(Admin admin, AccountStatus oldStatus, AccountStatus newStatus, SuperAdmin superAdmin, String reason) {
        try {
            AdminStatusChangeLog logEntry = AdminStatusChangeLog.builder()
                    .admin(admin)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .changedBySuperAdmin(superAdmin)
                    .changedAt(OffsetDateTime.now())
                    .reason(reason != null && !reason.isBlank() ? reason : "")
                    .build();

            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save admin status change log for adminId={}: {}", admin.getAdminId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all status change logs for a specific admin
     * @param adminId The admin ID
     * @return List of status change logs, ordered by most recent first
     */
    public List<AdminStatusChangeLog> getLogsByAdminId(Long adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("AdminId must not be null");
        }
        return logRepository.findByAdmin_AdminIdOrderByChangedAtDesc(adminId);
    }

    /**
     * Get all admin status change logs
     * @return List of all status change logs, ordered by most recent first
     */
    public List<AdminStatusChangeLog> getAllLogs() {
        return logRepository.findAllByOrderByChangedAtDesc();
    }
}

