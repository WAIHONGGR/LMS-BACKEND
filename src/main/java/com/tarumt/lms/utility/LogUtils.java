package com.tarumt.lms.utility;

public class LogUtils {

    /**
     * Mask an email for logging to protect PII
     * Example: john.doe@gmail.com -> j****@gmail.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "****";
        int index = email.indexOf("@");
        if (index <= 1) return "****";
        return email.substring(0, 1) + "****" + email.substring(index);
    }
}

