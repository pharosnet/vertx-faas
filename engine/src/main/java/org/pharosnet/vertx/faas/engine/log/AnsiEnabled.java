package org.pharosnet.vertx.faas.engine.log;

public enum AnsiEnabled {

    DETECT,

    /**
     * Enable ANSI-colored output.
     */
    ALWAYS,

    /**
     * Disable ANSI-colored output.
     */
    NEVER

}