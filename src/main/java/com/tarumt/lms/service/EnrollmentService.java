package com.tarumt.lms.service;

import com.tarumt.lms.model.Course;
import com.tarumt.lms.model.Enrollment;
import com.tarumt.lms.model.EnrollmentId;
import com.tarumt.lms.model.Student;
import com.tarumt.lms.repo.CourseRepository;
import com.tarumt.lms.repo.EnrollmentRepository;
import com.tarumt.lms.repo.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Enroll a student in a course
     */
    @Transactional
    public Enrollment enrollStudent(Long studentId, Long courseId) {
        log.info("Enrolling student {} in course {}", studentId, courseId);

        // Validate student exists
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            throw new RuntimeException("Student not found with ID: " + studentId);
        }

        // Validate course exists
        Optional<Course> courseOpt = courseRepository.findByCourseId(courseId);
        if (courseOpt.isEmpty()) {
            throw new RuntimeException("Course not found with ID: " + courseId);
        }

        Course course = courseOpt.get();

        // Check if course is active
        if (!"Active".equalsIgnoreCase(course.getStatus())) {
            throw new RuntimeException("Cannot enroll in a course that is not active. Course status: " + course.getStatus());
        }

        // Check if already enrolled
        if (enrollmentRepository.existsById_StudentIdAndId_CourseId(studentId, courseId)) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        // Create enrollment
        Enrollment enrollment = new Enrollment();
        EnrollmentId enrollmentId = new EnrollmentId();
        enrollmentId.setStudentId(studentId);
        enrollmentId.setCourseId(courseId);
        enrollment.setId(enrollmentId);
        enrollment.setStudent(studentOpt.get());
        enrollment.setCourse(course);
        enrollment.setStatus("Enrolled");
        enrollment.setProgress(0);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Student {} successfully enrolled in course {}", studentId, courseId);
        return savedEnrollment;
    }

    /**
     * Get all enrollments for a student
     */
    public List<Enrollment> getEnrollmentsByStudentId(Long studentId) {
        log.info("Fetching enrollments for student {}", studentId);
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Get enrollment by student and course
     */
    public Optional<Enrollment> getEnrollment(Long studentId, Long courseId) {
        log.info("Fetching enrollment for student {} and course {}", studentId, courseId);
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Check if student is enrolled in a course
     */
    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsById_StudentIdAndId_CourseId(studentId, courseId);
    }

    /**
     * Get enrollments by student and status
     */
    public List<Enrollment> getEnrollmentsByStudentIdAndStatus(Long studentId, String status) {
        log.info("Fetching enrollments for student {} with status {}", studentId, status);
        return enrollmentRepository.findByStudentIdAndStatus(studentId, status);
    }

    /**
     * Unenroll a student from a course (soft delete by changing status)
     */
    @Transactional
    public void unenrollStudent(Long studentId, Long courseId) {
        log.info("Unenrolling student {} from course {}", studentId, courseId);

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (enrollmentOpt.isEmpty()) {
            throw new RuntimeException("Enrollment not found");
        }

        Enrollment enrollment = enrollmentOpt.get();
        enrollment.setStatus("Withdrawn");
        enrollmentRepository.save(enrollment);
        log.info("Student {} successfully unenrolled from course {}", studentId, courseId);
    }
}

