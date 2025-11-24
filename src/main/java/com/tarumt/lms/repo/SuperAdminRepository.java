package com.tarumt.lms.repo;

import com.tarumt.lms.model.SuperAdmin;
import com.tarumt.lms.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

    Optional<SuperAdmin> findByEmail(String email);

    Optional<SuperAdmin> findByUserId(UUID userId);

    Optional<SuperAdmin> findByEmailAndStatus(String email, AccountStatus status);

    Optional<SuperAdmin> findByUserIdAndStatus(UUID userId, AccountStatus status);

    boolean existsByEmail(String email);
}
