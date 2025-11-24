package com.tarumt.lms.repo;

import com.tarumt.lms.model.UserStatusChangeLog;
import com.tarumt.lms.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStatusChangeLogRepository extends JpaRepository<UserStatusChangeLog, Long> {
    /**
     * Find all logs for a specific user type (STUDENT or INSTRUCTOR)
     * Ordered by most recent first
     */
    List<UserStatusChangeLog> findByUserTypeOrderByChangedAtDesc(Role userType);

    /**
     * Find all logs for a specific user ID
     * Ordered by most recent first
     */
    List<UserStatusChangeLog> findByUserIdOrderByChangedAtDesc(Long userId);

    /**
     * Find all logs for a specific user type and user ID
     * Ordered by most recent first
     */
    List<UserStatusChangeLog> findByUserTypeAndUserIdOrderByChangedAtDesc(Role userType, Long userId);
}
