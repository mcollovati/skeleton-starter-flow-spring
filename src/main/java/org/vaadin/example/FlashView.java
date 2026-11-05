package org.vaadin.example;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Element.flashClass now calls ElementJs.flashClass(className) instead of a
 * string executeJs. The client adds the class and removes it when the CSS
 * animation ends (see styles.css).
 */
@Route("flash")
@PermitAll
public class FlashView extends VerticalLayout {

    public FlashView() {
        Div box = new Div("Box");
        box.setId("box");
        box.addClassName("box");
        Button flash = new Button("Flash",
                e -> box.getElement().flashClass("flash"));
        flash.setId("flash");
        add(new H3("Element.flashClass"),
                new Paragraph("Click Flash. The box turns orange for one "
                        + "second, then the 'flash' class is removed again."),
                box, flash, new RouterLink("Back", MainView.class));
    }
}
