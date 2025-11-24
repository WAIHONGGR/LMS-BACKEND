package com.tarumt.lms.service;

import com.tarumt.lms.model.UserRole;
import com.tarumt.lms.repo.UserRoleRepository;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.service.user.InstructorService;
import com.tarumt.lms.service.user.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import com.tarumt.lms.model.enums.Role;

@Service
public class UserRoleService {

    @Autowired private StudentService studentService;
    @Autowired private InstructorService instructorService;
    @Autowired private UserRoleRepository userRoleRepository;

    @Transactional
    public Object registerUser(Role role, String email, String name, UUID supabaseUserId) {
        Object user;

        switch (role) {
            case STUDENT -> user = studentService.create(email, name, supabaseUserId);
            case INSTRUCTOR -> user = instructorService.create(email, name, supabaseUserId);
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        }

        // Create the user role to supabase for the new user
        UserRole userRole = UserRole.builder()
                .userId(supabaseUserId)
                .role(role)
                .build();
        userRoleRepository.save(userRole);

        return user;
    }

    @Transactional
    public UserRole createUserRole(UUID userId, Role role) {
        Optional<UserRole> existing = userRoleRepository.findByUserId(userId);

        if (existing.isPresent()) {
            return existing.get(); // Return existing role if already exists
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .role(role)
                .build();

        UserRole saved = userRoleRepository.save(userRole);
        return saved;
    }


}

