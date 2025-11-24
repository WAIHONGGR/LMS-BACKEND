package com.tarumt.lms.model;

import com.tarumt.lms.model.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(name = "admin")
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;
    private String name;

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
