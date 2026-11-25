package org.vaadin.example;

import java.util.List;

import com.acme.addon.AutoFocus;
import com.acme.addon.CharCounter;
import com.acme.addon.ConsoleLogger;
import com.acme.addon.EnterField;
import com.acme.addon.EscapeListener;
import com.acme.addon.EvalWidget;
import com.acme.addon.Highlighter;
import com.acme.addon.InlineHandler;
import com.acme.addon.LazyLoader;
import com.acme.addon.PageTitle;
import com.acme.addon.ResizeWatcher;
import com.acme.addon.ScrollPanel;
import com.acme.addon.Tooltip;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ContentSecurityPolicyViolationException;
import com.vaadin.flow.server.VaadinService;

/**
 * Each case uses one feature of the ACME add-on. Press "Run" to use it. The
 * view catches {@link ContentSecurityPolicyViolationException} only to show the result in
 * place; a real application would get an error instead (see
 * {@link PlainView}).
 */
@Route("")
public class CasesView extends Div {

    public CasesView() {
        add(new H1("ACME add-on under vaadin.contentSecurityPolicy="
                + VaadinService.getCurrent().getDeploymentConfiguration()
                        .getContentSecurityPolicyMode()));
        add(new Paragraph("The add-on (module 'addon') sends JavaScript as "
                + "strings. The application cannot change its code."));
        add(new Anchor("plain", "Open a view that uses AutoFocus "
                + "without catching anything"));

        add(new H3("Refused in Java (Flow sees the string API)"));
        addCase(1, "Element.executeJs in onAttach (AutoFocus)",
                (area) -> area.add(new AutoFocus()));
        addCase(2, "Element.executeJs, no result (ScrollPanel.scrollToBottom)",
                (area) -> {
                    ScrollPanel panel = filledPanel();
                    area.add(panel);
                    panel.scrollToBottom();
                });
        addCase(3, "Element.executeJs with result (ScrollPanel.getScrollTop)",
                (area) -> {
                    ScrollPanel panel = filledPanel();
                    area.add(panel);
                    Span result = new Span();
                    area.add(result);
                    panel.getScrollTop().thenAccept(
                            top -> result.setText("scrollTop=" + top));
                });
        addCase(4, "Page.executeJs with parameters (PageTitle)",
                (area) -> PageTitle.setWithBadge(UI.getCurrent(), "Inbox", 3));
        addCase(5, "Concatenated string (Highlighter)", (area) -> {
            // Flow takes a call site as "built from changing values" after 5
            // different strings, so send 5 colors from the same call site.
            // The message of the last one says that this is not supported.
            List<String> colors = List.of("orange", "green", "red", "purple",
                    "blue");
            for (int i = 0; i < colors.size(); i++) {
                Span span = new Span(colors.get(i));
                area.add(span, new Span(" "));
                if (i == colors.size() - 1) {
                    Highlighter.highlight(span, colors.get(i));
                    break;
                }
                try {
                    Highlighter.highlight(span, colors.get(i));
                } catch (ContentSecurityPolicyViolationException ex) {
                    // Go on, so that the same call site sends more strings
                }
            }
        });
        addCase(6, "Same text, varying parameter count (ConsoleLogger)",
                (area) -> {
                    ConsoleLogger.log(UI.getCurrent(), "one");
                    ConsoleLogger.log(UI.getCurrent(), "one", 2, true);
                });
        addCase(7, "setFilter with an expression (EscapeListener)",
                (area) -> {
                    Span result = new Span();
                    area.add(new EscapeListener(
                            () -> result.setText("Escape pressed")), result);
                });
        addCase(8, "addEventData with an expression (CharCounter)",
                (area) -> {
                    Span result = new Span();
                    area.add(new CharCounter(
                            n -> result.setText("trimmed length=" + n)),
                            result);
                });
        addCase(9, "addJsInitializer(String) (ResizeWatcher)",
                (area) -> area.add(new ResizeWatcher("width")));
        addCase(10, "JsFunction.of (Tooltip)", (area) -> {
            Span target = new Span("hover for tooltip");
            area.add(target);
            Tooltip.attach(target, "from JsFunction.of");
        });
        addCase(11, "Page.addDynamicImport(String) (LazyLoader)",
                (area) -> LazyLoader.load(UI.getCurrent()));

        add(new H3("Refused only by the browser (Flow cannot see it)"));
        addCase(12, "Inline onclick attribute (InlineHandler)",
                (area) -> area.add(new InlineHandler()));
        addCase(13, "new Function in the add-on's web component (EvalWidget)",
                (area) -> area.add(new EvalWidget("6 * 7")));

        add(new H3("Control: works under strict"));
        addCase(14, "@DomEvent filter + @EventData expression (EnterField)",
                (area) -> {
                    Span result = new Span();
                    EnterField field = new EnterField();
                    field.addEnterListener(e -> result
                            .setText("Enter: '" + e.getTrimmed() + "'"));
                    area.add(field, result);
                });
    }

    private void addCase(int number, String title,
            SerializableConsumer<Div> action) {
        Div row = new Div();
        row.setId("case-" + number);
        row.getStyle().set("margin", "0.5em 0").set("padding", "0.5em")
                .set("border", "1px solid #ddd");
        Span status = new Span("not run");
        status.setId("status-" + number);
        Div area = new Div();
        area.setId("area-" + number);
        NativeButton run = new NativeButton("Run", e -> {
            area.removeAll();
            try {
                action.accept(area);
                status.setText("no error in Java");
            } catch (ContentSecurityPolicyViolationException ex) {
                status.setText("REFUSED in Java: " + ex.getClass()
                        .getSimpleName());
                // What Flow says about the call, such as the method to
                // declare for it
                Pre message = new Pre(ex.getMessage());
                message.setId("message-" + number);
                message.getStyle().set("white-space", "pre-wrap");
                area.add(message);
            }
        });
        run.setId("run-" + number);
        row.add(new Span(number + ". " + title + " "), run, new Span(" "),
                status, area);
        add(row);
    }

    private static ScrollPanel filledPanel() {
        ScrollPanel panel = new ScrollPanel();
        for (int i = 1; i <= 20; i++) {
            panel.add(new Div("line " + i));
        }
        return panel;
    }
}
