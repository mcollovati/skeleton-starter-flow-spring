package org.vaadin.example.ecosystem;

import java.util.stream.Collectors;

import org.vaadin.jchristophe.SortableLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * sortable-layout (org.vaadin.jchristophe:sortable-layout 3.0.1). The add-on
 * sends its string on attach, so a refusal can not be caught here.
 */
@Route("eco/sortable")
public class SortableLayoutView extends EcoView {

    public SortableLayoutView() {
        super("sortable-layout");
        VerticalLayout list = new VerticalLayout();
        for (String name : new String[] { "A", "B", "C" }) {
            Div item = new Div(name);
            item.setId("item-" + name);
            item.getStyle().set("padding", "0.5em").set("border",
                    "1px solid #888");
            item.setWidth("100px");
            list.add(item);
        }
        SortableLayout sortable = withId(new SortableLayout(list), "sortable");
        Div order = output("order");
        sortable.addSortableComponentReorderListener(e -> order.setText(
                sortable.getComponents().stream().map(Component::getElement)
                        .map(element -> element.getText())
                        .collect(Collectors.joining(","))));
        add(sortable, order);
    }
}
