package org.vaadin.example.csp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

/**
 * Collects the warnings that Flow logs in the {@code vaadin.csp=warn} mode and
 * sorts each one by who made the call:
 * <ul>
 * <li>APP: application code called the API directly. The application must
 * change.</li>
 * <li>VAADIN: Flow or a Vaadin component made the call. Vaadin must change;
 * the report shows which application line triggered it.</li>
 * </ul>
 * Prints a summary after the test class. With
 * {@code -Dcsp.failOnAppViolations=true}, a test fails when it reaches an APP
 * call site.
 * <p>
 * Note: Flow logs each call site once per {@code VaadinService}. Browserless
 * tests create a new service for each test, so each test sees its own
 * warnings.
 */
public class CspViolationCollector implements BeforeAllCallback,
        BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final String REPORTER = "com.vaadin.flow.internal.CspViolationReporter";
    private static final String APP_PACKAGE = "org.vaadin.example.";

    /** Frames of the Flow APIs that report the call. */
    private static final List<String> API_PREFIXES = List.of(
            "com.vaadin.flow.internal.nodefeature.ElementListenerMap",
            "com.vaadin.flow.dom.", "com.vaadin.flow.component.page.Page.");

    public enum Owner {
        APP, VAADIN
    }

    public record Violation(Owner owner, String api, String callSite) {
        @Override
        public String toString() {
            return owner + "  " + api + "  <-  " + callSite;
        }
    }

    private final Appender appender = new Appender();
    /** Violations of the whole class, with the tests that reached them. */
    private final Map<Violation, Set<String>> all = new LinkedHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        appender.start();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        // Attached for each test: Spring Boot resets the logging
        // configuration when it starts the context, which drops appenders
        Logger logger = reporterLogger();
        if (logger.getAppender(appender.getName()) == null) {
            logger.addAppender(appender);
        }
        appender.current.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        List<Violation> found = List.copyOf(appender.current);
        found.forEach(v -> all
                .computeIfAbsent(v, k -> new TreeSet<>())
                .add(context.getDisplayName()));
        List<Violation> app = found.stream()
                .filter(v -> v.owner() == Owner.APP).distinct().toList();
        if (!app.isEmpty() && Boolean.getBoolean("csp.failOnAppViolations")) {
            throw new AssertionError(
                    "Code that does not work under a strict CSP:\n  "
                            + app.stream().map(Violation::toString)
                                    .collect(Collectors.joining("\n  ")));
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        reporterLogger().detachAppender(appender);
        StringBuilder report = new StringBuilder("\n===== CSP report for ")
                .append(context.getDisplayName()).append(" =====\n");
        if (all.isEmpty()) {
            report.append("No string JavaScript reached.\n");
        }
        for (Owner owner : Owner.values()) {
            all.forEach((v, tests) -> {
                if (v.owner() == owner) {
                    report.append(v).append("\n      reached by: ")
                            .append(tests).append('\n');
                }
            });
        }
        System.out.println(report);
    }

    /** The violations of the test that is running, for assertions. */
    public List<Violation> currentViolations() {
        return List.copyOf(appender.current);
    }

    private static Logger reporterLogger() {
        return (Logger) LoggerFactory.getLogger(REPORTER);
    }

    static Violation classify(StackTraceElementProxy[] frames) {
        // Skip the API frames: the first one names the API that reported
        int i = 0;
        String api = describe(frames[0].getStackTraceElement());
        while (i < frames.length && isApi(frames[i].getStackTraceElement())) {
            api = describe(frames[i].getStackTraceElement());
            i++;
        }
        StackTraceElement caller = i < frames.length
                ? frames[i].getStackTraceElement()
                : null;
        // First application frame, the line to look at
        StackTraceElement appFrame = null;
        for (int j = i; j < frames.length; j++) {
            StackTraceElement f = frames[j].getStackTraceElement();
            if (f.getClassName().startsWith(APP_PACKAGE)
                    && !f.getClassName().startsWith(
                            CspViolationCollector.class.getPackageName())
                    && !f.getClassName().endsWith("Test")) {
                appFrame = f;
                break;
            }
        }
        boolean appCalledApi = caller != null && appFrame == caller;
        String callSite;
        if (appCalledApi) {
            callSite = describe(caller);
        } else {
            callSite = describe(caller) + " (triggered from "
                    + (appFrame == null
                            ? "no application frame: test setup or deferred work"
                            : describe(appFrame))
                    + ")";
        }
        return new Violation(appCalledApi ? Owner.APP : Owner.VAADIN, api,
                callSite);
    }

    private static boolean isApi(StackTraceElement frame) {
        return API_PREFIXES.stream()
                .anyMatch(p -> frame.getClassName().startsWith(p)
                        || (frame.getClassName() + ".").startsWith(p));
    }

    private static String describe(StackTraceElement f) {
        if (f == null) {
            return "?";
        }
        String cls = f.getClassName();
        return cls.substring(cls.lastIndexOf('.') + 1) + "."
                + f.getMethodName() + "(" + f.getFileName() + ":"
                + f.getLineNumber() + ")";
    }

    private static final class Appender extends AppenderBase<ILoggingEvent> {
        private final List<Violation> current = new ArrayList<>();

        Appender() {
            setName("csp-violation-collector");
        }

        @Override
        protected synchronized void append(ILoggingEvent event) {
            IThrowableProxy error = event.getThrowableProxy();
            if (error != null
                    && error.getStackTraceElementProxyArray().length > 0) {
                current.add(classify(error.getStackTraceElementProxyArray()));
            }
        }
    }
}
