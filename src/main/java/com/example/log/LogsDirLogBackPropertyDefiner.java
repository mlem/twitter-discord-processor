package com.example.log;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.PropertyDefiner;
import ch.qos.logback.core.status.Status;
import com.example.file.DirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LogsDirLogBackPropertyDefiner implements PropertyDefiner {

    public static final String DEFAULT_DATA_LOGS = DirectoryManager.basePathRelativeToJar();
    private static Path logsDir;
    private static final Logger logger = LoggerFactory.getLogger(LogsDirLogBackPropertyDefiner.class);

    public static void setDirectoryManager(DirectoryManager directoryManager) {
        logsDir = directoryManager.getLogsDir();
    }

    @Override
    public String getPropertyValue() {
        if (logsDir == null) {
            logger.warn("no logs directory specified, using default " + DEFAULT_DATA_LOGS);
            return DEFAULT_DATA_LOGS;
        }
        return logsDir.toString();
    }

    @Override
    public void setContext(Context context) {

    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public void addStatus(Status status) {

    }

    @Override
    public void addInfo(String s) {

    }

    @Override
    public void addInfo(String s, Throwable throwable) {

    }

    @Override
    public void addWarn(String s) {

    }

    @Override
    public void addWarn(String s, Throwable throwable) {

    }

    @Override
    public void addError(String s) {

    }

    @Override
    public void addError(String s, Throwable throwable) {

    }
}
