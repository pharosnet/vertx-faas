package org.pharosnet.vertx.faas.core.commons;

import java.util.Optional;

public enum FaaSActive {

    LOCAL("local"),
    DEV("dev"),
    TEST("test"),
    STAGE("stage"),
    PROD("prod");

    public static final String ENV_KEY = "FAAS_ACTIVE";

    public static FaaSActive get() {
        String active = Optional.ofNullable(System.getenv(ENV_KEY)).orElse("").trim().toLowerCase();
        if ("local".equals(active)) {
            return LOCAL;
        }
        if ("dev".equals(active)) {
            return DEV;
        }
        if ("test".equals(active)) {
            return TEST;
        }
        if ("stage".equals(active)) {
            return STAGE;
        }
        if ("prod".equals(active)) {
            return PROD;
        }
        throw new RuntimeException(String.format("环境变量 (%s) 错误，请使用 local, dev, test, stage, prod 中的一个。", ENV_KEY));
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
