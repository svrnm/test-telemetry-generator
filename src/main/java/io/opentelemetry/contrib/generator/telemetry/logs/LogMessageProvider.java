package io.opentelemetry.contrib.generator.telemetry.logs;

import io.opentelemetry.contrib.generator.telemetry.logs.dto.LogMessages;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LogMessageProvider {

    private static Map<String, List<String>> severityReasonsMap;

    static {
        initializeSeverityMaps();
    }

    public static String getLogMessage(String severity) {
        List<String> possibleMessages = severityReasonsMap.entrySet().stream()
                .filter(severityMessages -> severityMessages.getKey().equalsIgnoreCase(severity))
                .findFirst().get().getValue();
        String plainMessage = possibleMessages.get(ThreadLocalRandom.current().nextInt(possibleMessages.size()));
        return getLog4j2Message(plainMessage, severity);
    }

    private static String getLog4j2Message(String message, String severity) {
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("TelemetryGenerator")
                .setLevel(Level.toLevel(severity))
                .setMessage(new SimpleMessage(message))
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                .build();
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%r [%t] %p %c %x - %m%n").build();
        return layout.toSerializable(logEvent);
    }

    private static void initializeSeverityMaps() {
        severityReasonsMap = new HashMap<>();
        List<String> infoMessages = LogMessages.infoMessages;
        List<String> warnMessages = LogMessages.warnMessages;
        List<String> errorMessages = LogMessages.errorMessages;
        List<String> debugMessages = LogMessages.debugMessages;

        severityReasonsMap.put(Constants.INFO_SEVERITY, infoMessages);
        severityReasonsMap.put(Constants.WARN_SEVERITY, warnMessages);
        severityReasonsMap.put(Constants.ERROR_SEVERITY, errorMessages);
        severityReasonsMap.put(Constants.DEBUG_SEVERITY, debugMessages);
        severityReasonsMap.put(Constants.TRACE_SEVERITY, debugMessages);
    }
}
