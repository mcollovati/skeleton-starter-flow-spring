package org.vaadin.example.review;

import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding 2: the new setShortcutListenOnElement(JsFunction, Component)
 * overload makes a call with a null literal ambiguous. Code that clears the
 * locator with setShortcutListenOnElement(null, component) compiles against
 * main and no longer compiles against the branch.
 * <p>
 * main: passes. Branch: fails, javac reports "reference to
 * setShortcutListenOnElement is ambiguous".
 */
class NullLocatorCompileTest {

    private static final String SOURCE = """
            import com.vaadin.flow.component.Shortcuts;
            import com.vaadin.flow.component.html.Div;

            class ClearsLocator {
                void clear(Div host) {
                    Shortcuts.setShortcutListenOnElement(null, host);
                }
            }
            """;

    @Test
    void nullLocatorLiteral_compiles() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String classPath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        Path out = Files.createTempDirectory("null-locator");
        StringWriter diagnostics = new StringWriter();
        SimpleJavaFileObject source = new SimpleJavaFileObject(
                URI.create("string:///ClearsLocator.java"),
                SimpleJavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreErrors) {
                return SOURCE;
            }
        };
        boolean compiled = compiler.getTask(diagnostics, null, null,
                List.of("-classpath", classPath, "-d", out.toString(),
                        "-Xlint:-removal", "-Xlint:-deprecation"),
                null, List.of(source)).call();
        assertTrue(compiled, diagnostics.toString());
    }
}
