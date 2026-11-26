package org.vaadin.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Calls a method that a {@code @JsDefinition} interface inherits from its
 * parent. Change the expression in the parent, apply with the dev loop and
 * press the button again: the text shows which version of the JavaScript ran.
 * Only Flow HTML components are used, since the Vaadin components of the
 * release are built against the released Flow API.
 */
@Route("")
public class MainView extends Div {

    public MainView() {
        add(new H3("Hotswap of a JavaScript definition parent"));
        add(new Paragraph("Edit the @JsExpression in PlainParentJs or"
                + " AnnotatedParentJs (v1 -> v2), apply, then press the"
                + " button again."));

        Span plain = new Span("not run");
        plain.setId("plain-result");
        NativeButton plainButton = new NativeButton(
                "Run PlainParentChildJs.show",
                e -> plain.getElement().executeJs(PlainParentChildJs.class)
                        .show("clicked"));
        plainButton.setId("plain");

        Span annotated = new Span("not run");
        annotated.setId("annotated-result");
        NativeButton annotatedButton = new NativeButton(
                "Run AnnotatedParentChildJs.show",
                e -> annotated.getElement()
                        .executeJs(AnnotatedParentChildJs.class)
                        .show("clicked"));
        annotatedButton.setId("annotated");

        Span direct = new Span("not run");
        direct.setId("direct-result");
        NativeButton directButton = new NativeButton("Run DirectJs.show",
                e -> direct.getElement().executeJs(DirectJs.class)
                        .show("clicked"));
        directButton.setId("direct");

        add(new Div(plainButton, plain), new Div(annotatedButton, annotated));
        add(new Div(directButton, direct));

        add(new H3("Methods without @JsExpression (vaadin/flow#27441)"));

        Span shorthandDirect = new Span("not run");
        shorthandDirect.setId("shorthand-direct-result");
        NativeButton shorthandDirectButton = new NativeButton(
                "Run ShorthandDirectJs.append", e -> {
                    shorthandDirect.setText("");
                    shorthandDirect.getElement()
                            .executeJs(ShorthandDirectJs.class)
                            .append("clicked");
                });
        shorthandDirectButton.setId("shorthand-direct");

        Span shorthandParent = new Span("not run");
        shorthandParent.setId("shorthand-parent-result");
        NativeButton shorthandParentButton = new NativeButton(
                "Run ShorthandParentChildJs.append", e -> {
                    shorthandParent.setText("");
                    shorthandParent.getElement()
                            .executeJs(ShorthandParentChildJs.class)
                            .append("clicked");
                });
        shorthandParentButton.setId("shorthand-parent");

        Span pageTitle = new Span("not run");
        pageTitle.setId("page-title-result");
        NativeButton pageTitleButton = new NativeButton(
                "Run Page.executeJs(PageTitleJs).setTitle", e -> {
                    try {
                        getUI().orElseThrow().getPage()
                                .executeJs(PageTitleJs.class)
                                .setTitle("title set");
                        pageTitle.setText("called");
                    } catch (IllegalArgumentException ex) {
                        pageTitle.setText("refused: " + ex.getMessage());
                    }
                });
        pageTitleButton.setId("page-title");

        Span shorthandParams = new Span("not run");
        shorthandParams.setId("shorthand-params-result");
        NativeButton shorthandParamsButton = new NativeButton(
                "Run ShorthandParamsJs.append", e -> {
                    shorthandParams.setText("");
                    // H4: change to .append("clicked", " twice") together
                    // with the parameters of ShorthandParamsJs.append
                    shorthandParams.getElement()
                            .executeJs(ShorthandParamsJs.class)
                            .append("clicked");
                });
        shorthandParamsButton.setId("shorthand-params");

        add(new Div(shorthandDirectButton, shorthandDirect),
                new Div(shorthandParentButton, shorthandParent),
                new Div(pageTitleButton, pageTitle),
                new Div(shorthandParamsButton, shorthandParams));
    }
}
