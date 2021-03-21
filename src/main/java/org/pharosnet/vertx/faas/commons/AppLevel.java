package org.pharosnet.vertx.faas.commons;

import java.util.Optional;

public enum AppLevel {

    DEV("dev"),
    TEST("test"),
    STAGE("stage"),
    PROD("prod");

    public static AppLevel get() {
        String level = Optional.ofNullable(System.getenv("FAAS_ACTIVE")).orElse("dev").trim().toLowerCase();
        if ("dev".equals(level)) {
            return DEV;
        }
        if ("test".equals(level)) {
            return TEST;
        }
        if ("stage".equals(level)) {
            return STAGE;
        }
        if ("prod".equals(level)) {
            return PROD;
        }
        throw new RuntimeException("FAAS_ACTIVE环境变量错误，请使用 DEV TEST STAGE PROD 中的一个。");
    }

    AppLevel(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
