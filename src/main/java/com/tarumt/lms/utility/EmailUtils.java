package com.tarumt.lms.utility;

import org.springframework.stereotype.Component;

@Component
public class EmailUtils {

    public static boolean isValidEmailDomain(String email, String domain) {
        return email != null && email.toLowerCase().endsWith(domain.toLowerCase());
    }
}

