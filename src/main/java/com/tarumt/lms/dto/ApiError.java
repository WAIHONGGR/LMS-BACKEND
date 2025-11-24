package com.tarumt.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {
    private String code;       // Optional error code
    private String message;    // Human-readable error
}
