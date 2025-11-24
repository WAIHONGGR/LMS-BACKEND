package com.tarumt.lms.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String name;
    private String supabaseUserId;
}

