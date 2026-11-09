package org.vaadin.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Reproducer: a resynchronization request sent over a WebSocket push
 * connection is never answered.
 * <p>
 * Push uses the {@code WEBSOCKET} transport, so the client sends its requests
 * over the WebSocket too. {@code drop-one-push.js} is inlined into the page
 * before the Flow client starts. It drops one message that the server pushes,
 * as a proxy restart or a network problem does.
 */
@SpringBootApplication
@Push(transport = Transport.WEBSOCKET)
@StyleSheet(Lumo.STYLESHEET)
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addInlineWithContents(Inline.Position.PREPEND,
                readResource("/drop-one-push.js"), Inline.Wrapping.JAVASCRIPT);
    }

    private static String readResource(String name) {
        try (InputStream in = Application.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
