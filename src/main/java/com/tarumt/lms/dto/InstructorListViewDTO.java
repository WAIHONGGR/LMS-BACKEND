package com.tarumt.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorListViewDTO {
    private Long instructorId;
    private String name;
    private String email;
    private String status;
    private OffsetDateTime registeredDate;
}