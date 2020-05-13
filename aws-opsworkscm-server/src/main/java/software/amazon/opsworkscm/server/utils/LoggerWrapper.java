package software.amazon.opsworkscm.server.utils;

import software.amazon.cloudformation.proxy.Logger;

public class LoggerWrapper {

    Logger logger;

    private static final String infoPrefix = "[INFO] ";
    private static final String errorPrefix = "[ERROR] ";

    public LoggerWrapper(Logger logger) {
        this.logger = logger;
    }

    public void log(final String message) {
        info(message);
    }

    public void info(final String message) {
        logger.log(infoPrefix + message);
    }

    public void error(final String message) {
        logger.log(errorPrefix + message);
    }

    public void error(final String message, final Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(errorPrefix).append(message).append('\n');
        sb.append(e.toString()).append('\n');
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            sb.append('\t').append("at ").append(stackTraceElement.toString()).append('\n');
        }
        logger.log(sb.toString());
    }

}
