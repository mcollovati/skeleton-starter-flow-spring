package org.vaadin.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

/**
 * Calls the string fallback interface {@link AcmeCspFallbacks} directly, as
 * application code may do once the interface exists (vaadin/flow#27078,
 * 3f8c95b4fd). The build registers its functions under the identifiers of
 * string fallbacks, so the bundle check must look for those, not for the
 * identifiers of the plain JavaScript. Otherwise the server logs that the
 * bundle does not carry JavaScript that it does carry.
 */
@Route("direct-fallback")
public class DirectFallbackView extends Div {

    public DirectFallbackView() {
        Div panel = new Div();
        panel.setId("panel");
        panel.getStyle().set("height", "60px").set("overflow", "auto")
                .set("border", "1px solid gray");
        for (int i = 1; i <= 20; i++) {
            panel.add(new Paragraph("Line " + i));
        }
        Paragraph result = new Paragraph();
        result.setId("result");
        NativeButton run = new NativeButton("Run", event -> {
            AcmeCspFallbacks js = panel.getElement()
                    .executeJs(AcmeCspFallbacks.class);
            js.scrollToBottom();
            js.scrollTop().then(Double.class,
                    top -> result.setText("scrollTop " + (top > 0 ? "> 0" : top)),
                    error -> result.setText("failed: " + error));
        });
        run.setId("run");
        add(panel, run, result);
    }
}
