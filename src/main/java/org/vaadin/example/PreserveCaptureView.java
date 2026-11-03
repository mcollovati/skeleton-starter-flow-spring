package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

/**
 * F6: a captured element in a view with {@link PreserveOnRefresh}.
 * <p>
 * The capture is encoded once, as {@code {"@v-node": <id>}}, when the
 * {@link DomEventData} is created. On refresh, the view moves to a new UI, and
 * its nodes get new ids there. The listener is sent again, but with the old
 * id, so the function gets whatever node has that id in the new UI.
 */
@Route("event-data-preserve")
@PreserveOnRefresh
public class PreserveCaptureView extends VerticalLayout {

    @JsDefinition
    public interface CaptureJs extends Serializable {
        /** The id of the element, or its tag and text if it has no id. */
        @JsExpression("return $0 === null ? 'null' : ($0.id || ('<' + $0.tagName + '> ' + $0.textContent));")
        DomEventData<String> describe(Element element);
    }

    private static final CaptureJs JS = JsSnippets.of(CaptureJs.class);

    private final Div target = new Div("captured target");
    private final NativeButton button = new NativeButton("Click");
    private final Div out = new Div();
    private final Div ids = new Div();
    private DomEventData<String> described;

    public PreserveCaptureView() {
        add(new H3("F6. Captured element with @PreserveOnRefresh"));
        add(new Paragraph("Click: expect 'target'. Refresh the page, then"
                + " click again. Expected: 'target' again. Bug: the capture"
                + " still has the old node id, so it finds another node,"
                + " for example the button itself. The line below the"
                + " button shows the node id of the target and the encoded"
                + " capture."));
        for (int i = 0; i < 5; i++) {
            add(new Div("filler " + i));
        }
        target.setId("target");
        out.setId("out");
        ids.setId("ids");
        add(target, button, out, ids);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // isInitialAttach() is true again in the new UI after a refresh, so
        // a field makes sure that the listener is added only once
        if (described == null) {
            described = JS.describe(target.getElement());
            button.getElement()
                    .addEventListener("click",
                            e -> out.setText(e.getEventData(described)))
                    .addEventData(described);
        }
        // Node ids are set when the attach is done, so read them after it
        attachEvent.getUI().access(() -> ids.setText("target node id="
                + target.getElement().getNode().getId() + ", captures="
                + described.getEncodedCaptures()));
    }
}
