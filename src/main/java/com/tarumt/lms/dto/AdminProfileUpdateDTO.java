package com.tarumt.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminProfileUpdateDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be <= 100 characters")
    private String name;

    @Size(max = 20, message = "Phone number must be <= 20 characters")
    @Pattern(regexp = "\\+?\\d{10,20}", message = "Invalid phone number")
    private String phoneNum;

    private String birthDate; // optional, yyyy-MM-dd
}
