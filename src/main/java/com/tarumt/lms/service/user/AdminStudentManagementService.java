package com.tarumt.lms.service.user;

import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.Student;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.Role;
import com.tarumt.lms.repo.StudentRepository;
import com.tarumt.lms.repo.UserStatusChangeLogRepository;
import com.tarumt.lms.model.UserStatusChangeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStudentManagementService {

    private final StudentRepository studentRepository;
    private final UserStatusChangeLogRepository userStatusChangeLogRepository;

    // =====================================================
    // GET ALL STUDENTS (OPTIONAL STATUS FILTER & SEARCH)
    // =====================================================
    public List<Student> getAllStudents(AccountStatus status, String search) {
        if (status != null && search != null && !search.isBlank()) {
            // Filter by status and search
            return studentRepository.findByStatusAndNameOrEmailContainingIgnoreCase(status, search);
        } else if (status != null) {
            // Filter by status only
            return studentRepository.findByStatus(status);
        } else if (search != null && !search.isBlank()) {
            // Search only
            return studentRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        } else {
            // Get all students
            return studentRepository.findAll();
        }
    }

    // =====================================================
    // GET STUDENT BY ID
    // =====================================================
    public Optional<Student> getStudentById(Long studentId) {
        return studentRepository.findById(studentId);
    }

    // =====================================================
    // UPDATE STUDENT STATUS (ACTIVE / INACTIVE)
    // =====================================================
    @Transactional
    public void updateStudentStatus(Long studentId, AccountStatus newStatus, Admin admin, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        AccountStatus oldStatus = student.getStatus();

        // Update student status
        student.setStatus(newStatus);

        // Update endDate if deactivating
        if (newStatus == AccountStatus.INACTIVE) {
            student.setEndDate(OffsetDateTime.now());
        } else if (newStatus == AccountStatus.ACTIVE) {
            student.setEndDate(null); // Clear endDate when activating
        }

        studentRepository.save(student);

        // Log the status change
        UserStatusChangeLog logEntry = new UserStatusChangeLog();
        logEntry.setUserType(Role.STUDENT);
        logEntry.setUserId(studentId);
        logEntry.setOldStatus(oldStatus != null ? oldStatus.toString() : null);
        logEntry.setNewStatus(newStatus.toString());
        logEntry.setChangedByAdmin(admin);
        logEntry.setChangedAt(OffsetDateTime.now());
        logEntry.setReason(reason != null && !reason.isBlank() ? reason : null);

        userStatusChangeLogRepository.save(logEntry);

        log.info("Student status updated: studentId={}, oldStatus={}, newStatus={}, reason={}",
                studentId, oldStatus, newStatus, reason);
    }
}

