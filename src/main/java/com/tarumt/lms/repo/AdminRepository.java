package com.tarumt.lms.repo;

import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AdminRepository extends JpaRepository<Admin,Long> {

    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByUserId(UUID userId);

    Optional<Admin> findByAdminId(Long adminId);

    Optional<Admin> findByAdminIdAndStatus(Long adminId, AccountStatus status);

    Optional<Admin> findByEmailAndStatus(String email, AccountStatus status);

    Optional<Admin> findByUserIdAndStatus(UUID userId, AccountStatus status);

    boolean existsByEmail(String email);

    List<Admin> findByStatus(AccountStatus status);
}
