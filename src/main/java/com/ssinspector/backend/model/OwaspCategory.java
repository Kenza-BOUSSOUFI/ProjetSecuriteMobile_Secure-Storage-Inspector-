package com.ssinspector.backend.model;

import lombok.Getter;

/**
 * Represents the OWASP Mobile Top 10 security categories.
 */
@Getter
public enum OwaspCategory {
    M1("M1", "Improper Platform Usage"),
    M2("M2", "Insecure Data Storage"),
    M3("M3", "Insecure Communication"),
    M4("M4", "Insecure Authentication"),
    M5("M5", "Insufficient Cryptography"),
    M6("M6", "Insecure Authorization"),
    M7("M7", "Client Code Quality"),
    M8("M8", "Code Tampering"),
    M9("M9", "Reverse Engineering"),
    M10("M10", "Extraneous Functionality");

    private final String id;
    private final String title;

    OwaspCategory(String id, String title) {
        this.id = id;
        this.title = title;
    }
}
