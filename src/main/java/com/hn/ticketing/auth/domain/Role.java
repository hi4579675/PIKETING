package com.hn.ticketing.auth.domain;

// 사용자 권한 enum
// 확장 계획은 없지만, Spring Security의 hasRole과 매핑되도록 ROLE_ 접두사를 붙임
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;

    Role(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
