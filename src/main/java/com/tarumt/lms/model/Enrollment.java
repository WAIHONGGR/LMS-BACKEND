package com.tarumt.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Entity
@Table(name = "enrollment")
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "enroll_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date enrollDate;

    @Column(name = "complete_date")
    @Temporal(TemporalType.DATE)
    private Date completeDate;

    @Column(name = "status", length = 20)
    private String status = "Enrolled";

    @Column(name = "progress")
    private Integer progress = 0; // 0-100

    @PrePersist
    protected void onCreate() {
        if (enrollDate == null) {
            enrollDate = new Date();
        }
        if (status == null) {
            status = "Enrolled";
        }
        if (progress == null) {
            progress = 0;
        }
    }
}

