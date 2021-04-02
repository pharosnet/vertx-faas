package org.pharosnet.vertx.faas.codegen.commons;

public class Strings {

    public static String appendBefore(String target, String content, int times) {
        StringBuilder targetBuilder = new StringBuilder(target);
        for (int i = 0; i < times; i++) {
            targetBuilder.insert(0, content);
        }
        target = targetBuilder.toString();
        return target;
    }

}
