package org.pharosnet.vertx.faas.engine.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;
import org.pharosnet.vertx.faas.commons.AppLevel;

public class LogbackColor extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        if (AppLevel.get().equals(AppLevel.DEV)) {
            Level level = event.getLevel();
            switch (level.levelInt) {
                //ERROR等级为红色
                case Level.ERROR_INT:
                    return ANSIConstants.RED_FG;
                //WARN等级为黄色
                case Level.WARN_INT:
                    return ANSIConstants.YELLOW_FG;
                //INFO等级为蓝色
                case Level.INFO_INT:
                    return ANSIConstants.GREEN_FG;
                //DEBUG等级为绿色
                case Level.DEBUG_INT:
                    return ANSIConstants.BLUE_FG;
                //DEBUG等级为绿色
                case Level.TRACE_INT:
                    return ANSIConstants.WHITE_FG;
                //其他为默认颜色
                default:
                    return ANSIConstants.DEFAULT_FG;
            }
        } else {
            return  ANSIConstants.DEFAULT_FG;
        }

    }

}
