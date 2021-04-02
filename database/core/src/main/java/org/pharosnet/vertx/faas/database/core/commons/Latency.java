package org.pharosnet.vertx.faas.database.core.commons;

import java.util.concurrent.TimeUnit;

public class Latency {

    public Latency() {
    }

    private long start;
    private long value;

    public void start() {
        this.start = System.nanoTime();
    }

    public Latency end() {
        this.value = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return this;
    }

    public String toString() {
        return this.value + "ms";
    }

    public long value() {
        return this.value;
    }

}
