package org.vaadin.example;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * A view that uses APIs that do not work under a strict Content Security
 * Policy (marked UNSAFE), and one that does (marked SAFE).
 */
@Route("csp")
public class CspView extends VerticalLayout {

    private final Paragraph log = new Paragraph();

    public CspView() {
        Button logButton = new Button("Log to console", e ->
        // UNSAFE: string JavaScript, compiled by the browser
        getUI().ifPresent(ui -> ui.getPage().executeJs("console.log($0)",
                "Button clicked")));
        logButton.setId("log");

        // UNSAFE: event data and filter expressions are compiled by the
        // browser
        getElement().addEventListener("keydown",
                e -> log.setText("Key: " + e.getEventData()
                        .get("event.key").asString()))
                .addEventData("event.key")
                .setFilter("event.key === 'Escape'");

        add(logButton, log);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // UNSAFE: string JavaScript
        getElement().executeJs("this.setAttribute('data-ready', '')");

        // SAFE: declared with @JsDefinition, collected by the build
        getElement().executeJs(TitleJs.class).setTitle("CSP demo");
    }
}
