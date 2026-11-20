package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.zoomist.Zoomist;
import com.vaadin.flow.router.Route;

/**
 * Zoomist (com.flowingcode.vaadin.addons:zoomist-addon 1.3.0).
 */
@Route("eco/zoomist")
public class ZoomistView extends EcoView {

    public ZoomistView() {
        super("Zoomist");
        Zoomist zoomist = withId(new Zoomist("images/eco-sample.png"),
                "zoomist");
        zoomist.getElement().getStyle().set("width", "400px");
        zoomist.getElement().getStyle().set("height", "300px");
        add(zoomist);
        var width = output("width");
        add(button("container-data", "Container data",
                () -> zoomist.getContainerData(data -> width
                        .setText("width=" + Math.round(data.getWidth())))),
                width);
    }
}
