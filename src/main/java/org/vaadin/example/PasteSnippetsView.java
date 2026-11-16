package org.vaadin.example;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.PasteEvent;
import com.vaadin.flow.component.clipboard.PasteFileHandler;
import com.vaadin.flow.component.clipboard.PasteOptions;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * The clipboard paste handling of vaadin/flow#26889: the pasted text and HTML
 * are read as declared event data, the "skip editable fields" filter is a
 * declared filter, and the file upload installer goes through
 * {@code Element.addJsInitializer(JsFunction)}.
 */
@Route("paste-snippets")
public class PasteSnippetsView extends Div {

    public PasteSnippetsView() {
        setId("paste-snippets");

        // P1: paste on a focusable element, text and HTML
        Div target = new Div("P1 focus me and paste");
        target.setId("p1-target");
        target.getElement().setAttribute("tabindex", "0");
        Div p1Result = new Div();
        p1Result.setId("p1-result");
        Clipboard.onPaste(target, event -> p1Result.setText(
                "text=" + event.getText() + ";html=" + event.getHtml()));

        // P2: a paste listener on the view skips editable fields by default
        Div p2Target = new Div("P2 focus me and paste");
        p2Target.setId("p2-target");
        p2Target.getElement().setAttribute("tabindex", "0");
        Input p2Input = new Input();
        p2Input.setId("p2-input");
        Div p2Result = new Div();
        p2Result.setId("p2-result");
        Clipboard.onPaste(this, PasteOptions.defaults(),
                event -> p2Result.setText(describe(event)));

        // P3: file paste, uploaded by the installer that
        // addJsInitializer(JsFunction) installs
        Div p3Target = new Div("P3 focus me and paste a file");
        p3Target.setId("p3-target");
        p3Target.getElement().setAttribute("tabindex", "0");
        Div p3Result = new Div();
        p3Result.setId("p3-result");
        AtomicInteger files = new AtomicInteger();
        Clipboard.onFilePaste(p3Target,
                PasteFileHandler.batch()
                        .onFile(file -> p3Result.setText("files="
                                + files.incrementAndGet() + ";last="
                                + file.fileName()))
                        .build());

        add(new Paragraph("Run in production mode with vaadin.csp=strict and keep "
                + "the browser console open. No case may log a Content "
                + "Security Policy violation or an error."));
        section("P1 paste text and HTML",
                "Copy some bold text from a web page, click the P1 box "
                        + "and paste with Ctrl+V. Expected: text=<the text>;"
                        + "html=<its HTML>. Plain text from an editor gives "
                        + "html=null.",
                target, p1Result);
        section("P2 a paste in an editable field is skipped",
                "Copy some text. Paste it in the field: nothing is "
                        + "reported. Click the P2 box and paste. Expected: "
                        + "target=p2-target;text=<the text>.",
                p2Target, p2Input, p2Result);
        section("P3 paste a file",
                "Copy an image (right-click an image in a web page, Copy "
                        + "image), click the P3 box and paste. Expected: "
                        + "files=1;last=image.png. A file copied in a file "
                        + "manager may only paste as text on Linux.",
                p3Target, p3Result);
    }

    private void section(String title, String instructions,
            Component... content) {
        Div row = new Div(new H3(title));
        Paragraph how = new Paragraph(instructions);
        how.getStyle().set("margin", "0 0 0.5em").set("color", "gray");
        row.add(how);
        row.add(content);
        add(row);
    }

    private static String describe(PasteEvent event) {
        Element target = event.getTargetElement();
        String id = target != null ? target.getAttribute("id") : null;
        return "target=" + id + ";text=" + event.getText();
    }
}
