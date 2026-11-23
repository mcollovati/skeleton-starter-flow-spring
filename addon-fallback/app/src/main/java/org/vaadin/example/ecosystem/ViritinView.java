package org.vaadin.example.ecosystem;

import org.vaadin.firitin.components.RichText;
import org.vaadin.firitin.components.orderedlayout.VScroller;
import org.vaadin.firitin.util.WebStorage;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * flow-viritin (in.virit:viritin 3.9.1): RichText, VScroller and WebStorage.
 */
@Route("eco/viritin")
public class ViritinView extends EcoView {

    public ViritinView() {
        super("flow-viritin");
        // "this.innerHTML = $0"
        run(() -> add(withId(
                new RichText().withSafeHtml("<p>Hello <b>Viritin</b></p>"),
                "rich")));

        // A script on attach that fires scroll-to-end, and setScrollTop
        Div tall = new Div("top");
        tall.setHeight("2000px");
        VScroller scroller = withId(new VScroller(tall), "scroller");
        scroller.setHeight("200px");
        Div end = output("end");
        scroller.addScrollToEndListener(e -> end.setText("end reached"));
        add(scroller, end);
        add(button("scroll", "Scroll", () -> scroller.setScrollTop(1900)));

        // WebStorage set and get
        Div stored = output("stored");
        add(button("store", "Store", () -> {
            WebStorage.setItem("viritin-csp", "works");
            WebStorage.getItem("viritin-csp", stored::setText);
        }), stored);
    }
}
