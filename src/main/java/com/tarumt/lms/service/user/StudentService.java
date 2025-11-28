package com.tarumt.lms.service.user;

import com.tarumt.lms.dto.StudentProfileUpdateDTO;
import com.tarumt.lms.model.Student;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.repo.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    // ================================
    // Basic CRUD Operations
    // ================================

    public Optional<Student> getByEmail(String email) {
        log.info("Fetching student profile for email={}", email);
        return studentRepository.findByEmail(email)
                .map(student -> {
                    log.info("Student found: id={}, email={}", student.getStudentId(), student.getEmail());
                    return student;
                })
                .or(() -> {
                    log.warn("Student not found for email={}", email);
                    return Optional.empty();
                });
    }


    public Optional<Student> getById(Long studentId) {
        log.info("Fetching student profile for studentId={}", studentId);
        return studentRepository.findByStudentId(studentId)
                .map(student -> {
                    log.info("Student found: id={}", student.getStudentId());
                    return student;
                })
                .or(() -> {
                    log.warn("Student not found for studentId={}", studentId);
                    return Optional.empty();
                });
    }


    public Optional<Student> getByUserId(UUID userId) {
        log.info("Fetching student profile for userId={}", userId);
        return studentRepository.findByUserId(userId)
                .map(student -> {
                    log.info("Student found: id={}, userId={}", student.getStudentId(), student.getUserId());
                    return student;
                })
                .or(() -> {
                    log.warn("Student not found for userId={}", userId);
                    return Optional.empty();
                });
    }

    @Transactional
    public Student create(String email, String name, UUID supabaseUserId) {
        log.info("Creating new student: email={}, supabaseUserId={}", email, supabaseUserId);
        Student student = new Student();
        student.setUserId(supabaseUserId);
        student.setEmail(email);
        student.setName(name);
        Student savedStudent = studentRepository.save(student);
        log.info("Student created successfully: id={}", savedStudent.getStudentId());
        return savedStudent;
    }


    public Optional<Student> getActiveStudentById(Long studentId) {
        log.info("Fetching active student profile for studentId={}", studentId);
        return studentRepository.findByStudentIdAndStatus(studentId, AccountStatus.ACTIVE)
                .map(student -> {
                    log.info("Active student found: studentId={}, email={}", student.getStudentId(), student.getEmail());
                    return student;
                })
                .or(() -> {
                    log.warn("Active student not found for studentId={}", studentId);
                    return Optional.empty();
                });
    }

    public Optional<Student> getActiveByEmail(String email) {
        log.info("Fetching ACTIVE student by email={}", email);
        return studentRepository.findByEmailAndStatus(email, AccountStatus.ACTIVE)
                .map(student -> {
                    log.info("Active student found: studentId={}, email={}", student.getStudentId(), student.getEmail());
                    return student;
                })
                .or(() -> {
                    log.warn("No active student found for email={}", email);
                    return Optional.empty();
                });
    }

    public Optional<Student> getActiveByUserId(UUID userId) {
        log.info("Fetching ACTIVE student by userId={}", userId);
        return studentRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
                .map(student -> {
                    log.info("Active student found: studentId={}, userId={}", student.getStudentId(), student.getUserId());
                    return student;
                })
                .or(() -> {
                    log.warn("No active student found for userId={}", userId);
                    return Optional.empty();
                });
    }


    @Transactional
    public Student updateStudentProfile(Long studentId, StudentProfileUpdateDTO dto) {
        log.info("Updating student profile for studentId={}, updateData={}", studentId, dto);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID {}", studentId);
                    return new RuntimeException("Student not found with ID " + studentId);
                });

        student.setName(dto.getName().trim());
        student.setPhoneNum(dto.getPhoneNum() != null && dto.getPhoneNum().isBlank() ? null : dto.getPhoneNum());

        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date parsedDate = sdf.parse(dto.getBirthDate());

                if (parsedDate.after(new Date())) {
                    log.warn("Birth date cannot be in the future: {}", dto.getBirthDate());
                    throw new RuntimeException("Birth date cannot be in the future");
                }

                student.setBirthDate(parsedDate);
            } catch (ParseException e) {
                log.error("Invalid birth date format: {}", dto.getBirthDate(), e);
                throw new RuntimeException("Invalid birth date format: " + dto.getBirthDate());
            }
        } else {
            student.setBirthDate(null);
        }

        Student savedStudent = studentRepository.save(student);
        log.info("Student profile updated successfully: studentId={}", savedStudent.getStudentId());
        return savedStudent;
    }



}
