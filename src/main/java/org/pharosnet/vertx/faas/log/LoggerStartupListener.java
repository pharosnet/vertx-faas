package org.pharosnet.vertx.faas.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import org.pharosnet.vertx.faas.commons.AppLevel;

import java.util.Optional;

public class LoggerStartupListener  extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private boolean isStarted = false;

    @Override
    public void start() {
        if(isStarted){
            return;
        }

        context.putProperty("APP_VERSION", Optional.ofNullable(System.getenv("FAAS_APP_VERSION")).orElse("EMPTY").trim());

        AppLevel level = AppLevel.get();
        Context context = getContext();
        context.putProperty("ACTIVE_PROFILE", level.getValue().toLowerCase());
        switch (level) {
            case DEV:
                context.putProperty("ACTIVE_PROFILE", level.getValue());
            case TEST:
                context.putProperty("ACTIVE_PROFILE", level.getValue());
            case STAGE:
                context.putProperty("ACTIVE_PROFILE", level.getValue());
            case PROD:
                context.putProperty("ACTIVE_PROFILE", level.getValue());
            default:
                context.putProperty("ACTIVE_PROFILE", level.getValue());
        }


        isStarted = true;
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onReset(LoggerContext context) {

    }

    @Override
    public void onStop(LoggerContext context) {

    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }
}
