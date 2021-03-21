package org.pharosnet.vertx.faas.commons;

import java.util.Optional;

public class BytesUtil {
    /**
     * parse byte length string
     *
     * @param value such as 2kb, 2mb, 2gb
     * @return byte length
     * @throws IllegalArgumentException illegal argument
     */
    public static long parseByteSize(String value) throws IllegalArgumentException {
        value = Optional.ofNullable(value).orElse("").trim();
        if (value.equals("") || value.length() < 3) {
            throw new IllegalArgumentException("parse byte size failed, value is illegal");
        }
        long size = 0L;
        try {
            String sizeValue = value.substring(0, value.length() - 2);
            size = Long.parseLong(sizeValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("parse byte size failed, value is illegal");
        }
        String sizeUnit = value.substring(value.length() - 2).toLowerCase();
        switch (sizeUnit) {
            case "kb":
                size = size * 1024;
                break;
            case "mb":
                size = size * 1024 * 1024;
                break;
            case "gb":
                size = size * 1024 * 1024 * 1024;
                break;
            default:
                throw new IllegalArgumentException("parse byte size failed, value is illegal");
        }

        return size;
    }


}
