package com.tarumt.lms.model;

import com.tarumt.lms.model.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@Entity
@Table(name = "user_status_change_log")
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private Role userType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_admin", nullable = false)
    private Admin changedByAdmin;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    @Column(name = "reason", length = 500)
    private String reason;
}