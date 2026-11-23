package org.vaadin.example;

import org.vaadin.example.browserless.CspView;
import org.vaadin.example.browserless.SkeletonView;
import org.vaadin.example.bundledeventdata.BundledEventDataView;
import org.vaadin.example.components.ChartsCspView;
import org.vaadin.example.components.ChartsDeclaredFunctionView;
import org.vaadin.example.components.GridCspView;
import org.vaadin.example.domfilters.DomEventFilterView;
import org.vaadin.example.errors.DeclaredJsErrorsView;
import org.vaadin.example.eventdata.EventDataView;
import org.vaadin.example.eventdata.PreserveCaptureView;
import org.vaadin.example.eventdata.SideEffectsView;
import org.vaadin.example.eventdatapath.EventDataElementView;
import org.vaadin.example.eventdatapath.EventDataPathView;
import org.vaadin.example.executejs.ExecuteJsCasesView;
import org.vaadin.example.shortcuts.ManyShortcutsView;
import org.vaadin.example.shortcuts.ShortcutCasesView;
import org.vaadin.example.snippets.DynamicImportView;
import org.vaadin.example.snippets.SnippetView;
import org.vaadin.example.triggers.InitializerSnippetsView;
import org.vaadin.example.triggers.PasteSnippetsView;
import org.vaadin.example.triggers.TriggerSnippetsView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Index of all CSP cases. Each group comes from one reproducer branch; see
 * the notes file in {@code docs/} for the expected results.
 */
@Route("")
@AnonymousAllowed
public class MainView extends VerticalLayout {

    public MainView() {
        add(new H2("CSP reproducers"));
        add(new Paragraph("The application runs with vaadin.csp=strict. "
                + "Use -Dvaadin.csp=warn to see the warnings instead."));

        group("Declared JavaScript snippets (vaadin/flow#26830)",
                "docs/CSP-NOTES.md, docs/DYNAMIC-IMPORT-NOTES.md",
                link("Snippets", SnippetView.class),
                link("Dynamic import", DynamicImportView.class));
        group("Trigger, paste and initializer snippets (vaadin/flow#26889)",
                "docs/TRIGGER-SNIPPETS-NOTES.md",
                link("Triggers", TriggerSnippetsView.class),
                link("Paste", PasteSnippetsView.class),
                link("Initializers", InitializerSnippetsView.class));
        group("Errors of declared JavaScript (vaadin/flow#27025)",
                "docs/DECLARED-JS-ERRORS-NOTES.md",
                link("Declared JS errors", DeclaredJsErrorsView.class));
        group("Simple executeJs call sites (vaadin/flow#26699)",
                "docs/PR-26699-NOTES.md (needs login: user / user)",
                link("Cases", ExecuteJsCasesView.class));
        group("Event data snippets (vaadin/flow#25658)",
                "docs/EVENT-DATA-NOTES.md",
                link("Event data", EventDataView.class),
                link("Preserve capture", PreserveCaptureView.class),
                link("Side effects", SideEffectsView.class));
        group("addEventDataPath (vaadin/flow#26999, #27001)",
                "docs/EVENT-DATA-PATH-NOTES.md",
                link("Event data path", EventDataPathView.class),
                link("Event data element", EventDataElementView.class));
        group("Bundled @EventData (vaadin/flow#27000)",
                "docs/BUNDLED-EVENT-DATA-NOTES.md",
                link("Bundled event data", BundledEventDataView.class));
        group("Bundled @DomEvent filters (vaadin/flow#27023)",
                "docs/DOM-EVENT-FILTER-NOTES.md",
                link("DOM event filters", DomEventFilterView.class));
        group("Shortcuts as declared JavaScript (vaadin/flow#26953)",
                "docs/SHORTCUT-NOTES.md",
                link("Cases", ShortcutCasesView.class),
                link("Many shortcuts", ManyShortcutsView.class));
        group("Components under strict CSP", "docs/GRID-CSP-NOTES.md, "
                + "docs/CHARTS-CSP-NOTES.md",
                link("Grid", GridCspView.class),
                link("Charts", ChartsCspView.class),
                link("Charts, declared function",
                        ChartsDeclaredFunctionView.class));
        group("Browserless tests (src/test/.../browserless)",
                "docs/CSP-BROWSERLESS.md",
                link("CSP view", CspView.class),
                link("Skeleton view", SkeletonView.class));
    }

    private void group(String title, String notes, Component... links) {
        Div group = new Div(new H4(title), new Paragraph(notes));
        group.addClassName("case");
        group.add(links);
        add(group);
    }

    private static Component link(String text,
            Class<? extends Component> view) {
        Div div = new Div(new RouterLink(text, view));
        return div;
    }
}
