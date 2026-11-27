package org.vaadin.example;

import org.vaadin.example.shorthand.MissingShorthandView;
import org.vaadin.example.shorthand.ShorthandView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

/**
 * A menu on the left with every use case, used by all routes through
 * {@link Layout}. Only Flow HTML components, so the menu works with any Flow
 * build that the tests run on.
 */
@Layout
public class MainLayout extends Div implements RouterLayout {

    private final Div content = new Div();

    public MainLayout() {
        Nav menu = new Nav();
        menu.addClassName("menu");
        menu.add(group("Add-on CSP fallbacks (#26153)"));
        menu.add(link("Cases 1-15", CasesView.class));
        menu.add(link("AutoFocus without catching", PlainView.class));
        menu.add(group("Optimized bundle (#27077, #27078)"));
        menu.add(link("Definition used only by reflection",
                MissingDefinitionView.class));
        menu.add(link("Direct call to a string fallback",
                DirectFallbackView.class));
        menu.add(group("Methods without @JsExpression (#27441)"));
        menu.add(link("Shorthand calls and refusals", ShorthandView.class));
        menu.add(link("Shorthand definition used only by reflection",
                MissingShorthandView.class));

        content.addClassName("content");
        addClassName("main-layout");
        add(menu, content);
    }

    private static Paragraph group(String title) {
        Paragraph paragraph = new Paragraph(title);
        paragraph.addClassName("menu-group");
        return paragraph;
    }

    private static RouterLink link(String text,
            Class<? extends Component> view) {
        return new RouterLink(text, view);
    }

    @Override
    public void showRouterLayoutContent(HasElement view) {
        content.getElement().removeAllChildren();
        if (view != null) {
            content.getElement().appendChild(view.getElement());
        }
    }
}
