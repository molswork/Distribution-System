package com.example.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBcryptTest {

    private static void printHash(BCryptPasswordEncoder enc, String label, String raw) {
        String hash = enc.encode(raw);
        System.out.println(label + "=" + hash);
    }

    @Test
    public void generate() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        printHash(enc, "BCRYPT_OF_Director123!", "Director123!");
        printHash(enc, "BCRYPT_OF_Leader123!", "Leader123!");
        printHash(enc, "BCRYPT_OF_Sales123!", "Sales123!");
        printHash(enc, "BCRYPT_OF_Agent123!", "Agent123!");
        printHash(enc, "BCRYPT_OF_admin123", "admin123");
        printHash(enc, "BCRYPT_OF_sales123", "sales123");
    }
}

