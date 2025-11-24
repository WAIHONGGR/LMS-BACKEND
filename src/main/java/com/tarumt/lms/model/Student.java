package com.tarumt.lms.model;

import com.tarumt.lms.model.enums.AccountStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "student")
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be <= 100 characters")
    private String name;

    @Size(max = 20, message = "Phone number must be <= 20 characters")
    @Pattern(regexp = "\\+?\\d{10,20}", message = "Invalid phone number")
    @Column(name = "phone_num")
    private String phoneNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "birth_date")
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Column(name = "registered_date")
    private OffsetDateTime registeredDate = OffsetDateTime.now();

    @Column(name = "end_date")
    private OffsetDateTime endDate;

}

