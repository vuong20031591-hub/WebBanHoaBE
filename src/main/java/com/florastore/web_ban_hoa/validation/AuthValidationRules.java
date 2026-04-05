package com.florastore.web_ban_hoa.validation;

public final class AuthValidationRules {

    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be at least 8 characters, include 1 uppercase letter, and 1 special character.";
    public static final String PHONE_MESSAGE =
            "Phone number must be exactly 10 digits and start with 0.";

    private AuthValidationRules() {
    }

    public static void validatePassword(String password) {
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException(PASSWORD_MESSAGE);
        }
    }

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }

        String digits = phone.replaceAll("\\D", "");

        if (digits.startsWith("840")) {
            digits = digits.substring(2);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        } else if (digits.startsWith("84") && digits.length() == 12) {
            digits = digits.substring(2);
        }

        if (!digits.matches("^0\\d{9}$")) {
            throw new IllegalArgumentException(PHONE_MESSAGE);
        }

        return digits;
    }

    public static String normalizePhoneOrDefault(String phone, String fallbackPhone) {
        try {
            return normalizePhone(phone);
        } catch (IllegalArgumentException ex) {
            return fallbackPhone;
        }
    }
}
