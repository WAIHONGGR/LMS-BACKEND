package com.tarumt.lms.model;

import com.tarumt.lms.model.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@Entity
@Table(name = "admin_status_change_log")
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatusChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin; // The admin whose status was changed

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private AccountStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private AccountStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_superadmin_id", nullable = false)
    private SuperAdmin changedBySuperAdmin; // The superadmin performing the change

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;
}
