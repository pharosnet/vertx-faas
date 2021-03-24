package org.pharosnet.vertx.faas.commons;

import java.util.Optional;

public enum FaaSActive {

    LOCAL("local"),
    DEV("dev"),
    TEST("test"),
    STAGE("stage"),
    PROD("prod");

    public static FaaSActive get() {
        String level = Optional.ofNullable(System.getenv("FAAS_ACTIVE")).orElse("local").trim().toLowerCase();
        if ("local".equals(level)) {
            return LOCAL;
        }
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
        throw new RuntimeException("FAAS_ACTIVE环境变量错误，请使用 local, dev, test, stage, prod, 中的一个。");
    }

    FaaSActive(String value) {
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
