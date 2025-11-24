package com.tarumt.lms.repo;

import com.tarumt.lms.model.AdminStatusChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminStatusChangeLogRepository extends JpaRepository<AdminStatusChangeLog, Long> {
    // Additional query methods can be added here if needed

    /**
     * Find all logs for a specific admin
     * Ordered by most recent first
     */
    List<AdminStatusChangeLog> findByAdmin_AdminIdOrderByChangedAtDesc(Long adminId);


    /**
     * Find all admin status change logs
     * Ordered by most recent first
     */
    List<AdminStatusChangeLog> findAllByOrderByChangedAtDesc();
}

