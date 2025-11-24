package com.tarumt.lms.model;

import com.tarumt.lms.model.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "superadmin")
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long superAdminId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "registered_date")
    private OffsetDateTime registeredDate = OffsetDateTime.now();

}

