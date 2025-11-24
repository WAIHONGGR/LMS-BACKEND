package com.tarumt.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.tarumt.lms.model.enums.QualificationStatus;
import java.time.OffsetDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "instructorqualification")
public class InstructorQualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qualification_level", nullable = false)
    private String qualificationLevel;

    @Column(name = "certificate_document")
    private String certificateDocument; // URL to Supabase, optional

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QualificationStatus status = QualificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Generic admin who performed the last action (approve or reject)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "made_by_admin")
    private Admin madeByAdmin;

    // Timestamp of the last action
    @Column(name = "made_at")
    private OffsetDateTime madeAt;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    // Optional helper methods
    public boolean isPending() {
        return this.status == QualificationStatus.PENDING;
    }

    public boolean isRejected() {
        return this.status == QualificationStatus.REJECTED;
    }

    public boolean isVerified() {
        return this.status == QualificationStatus.VERIFIED;
    }
}
