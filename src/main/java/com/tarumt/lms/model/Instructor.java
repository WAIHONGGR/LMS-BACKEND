package com.tarumt.lms.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarumt.lms.model.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "instructor")
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long instructorId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;
    private String name;

    @Column(name = "phone_num")
    private String phoneNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "birth_date")
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Column(name = "registered_date")
    private OffsetDateTime registeredDate = OffsetDateTime.now();

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @JsonIgnore
    @Column(name = "digital_signature")
    private String digitalSignature; // URL to Supabase

    @JsonIgnore
    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InstructorQualification> qualifications = new ArrayList<>();
}
