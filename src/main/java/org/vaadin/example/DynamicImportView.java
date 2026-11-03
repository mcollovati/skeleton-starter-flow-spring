package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Use cases for {@code Page.addDynamicImport(JsFunction)} (PR #26813).
 * <p>
 * A large web component is loaded only on first use. Its module is imported by
 * a declared snippet, so the bundler sees the {@code import()} and splits the
 * module into its own chunk, and nothing is compiled from a string, so it works
 * without {@code unsafe-eval}. The import blocks the changes of the same round
 * trip, so the element is defined before Flow creates it.
 */
@Route("dynamic-import")
public class DynamicImportView extends VerticalLayout {

    /** The snippets passed to addDynamicImport. */
    @JsDefinition
    public interface ImportJs extends Serializable {

        /** The path is a literal, so that the bundler can split it. */
        @JsExpression("return import('Frontend/report-editor/report-editor.js');")
        JsFunction loadEditor();

        /** Captures an element, which addDynamicImport rejects. */
        @JsExpression("return Promise.resolve($0);")
        JsFunction withElement(Element element);
    }

    /** A declared call that reports the state of the page into this. */
    @JsDefinition
    public interface StatusJs extends Serializable {
        @JsExpression("this.textContent = 'module evaluated: '"
                + " + !!window.reportEditorModuleEvaluated"
                + " + ', report-editor defined: '"
                + " + !!customElements.get('report-editor');")
        void show();
    }

    /**
     * The large component. No {@code @JsModule}: that would put the module in
     * the chunk of this view, loaded when the view opens rather than on first
     * use.
     */
    @Tag("report-editor")
    public static class ReportEditor extends Component {
        public ReportEditor(String report) {
            getElement().setAttribute("report", report);
        }
    }

    private static final ImportJs IMPORT_JS = JsSnippets.of(ImportJs.class);

    private int editors;

    public DynamicImportView() {
        add(new H3("addDynamicImport(JsFunction) - lazy module on first use"));

        // 1. Before the click the module is not loaded
        Div status = output("status");
        Button refresh = new Button("Refresh status",
                e -> status.getElement().executeJs(StatusJs.class).show());
        refresh.setId("refresh-status");
        status.getElement().executeJs(StatusJs.class).show();
        add(new Paragraph("Status (expect before the click: module evaluated:"
                + " false, report-editor defined: false)"), status, refresh);

        // 2. The click imports the module and adds the element in the same
        // round trip. Clicking again adds another editor; the import is the
        // same function with the same captures, so it is sent only once.
        Div editorsArea = output("editors");
        Button open = new Button("Open advanced editor", e -> {
            getUI().ifPresent(ui -> ui.getPage()
                    .addDynamicImport(IMPORT_JS.loadEditor()));
            editors++;
            ReportEditor editor = new ReportEditor("Q" + editors + " report");
            editor.setId("editor-" + editors);
            editorsArea.add(editor);
        });
        open.setId("open-editor");
        add(new Paragraph("Editors (expect: report-editor for \"Q1 report\" -"
                + " defined before Flow created it: yes)"), open, editorsArea);

        // 3. A function that captures an element is rejected, attached or not
        Div capture = output("capture-result");
        Button attached = new Button("Capture an attached element",
                e -> tryCapture(capture, capture.getElement()));
        attached.setId("capture-attached");
        Button detached = new Button("Capture a detached element",
                e -> tryCapture(capture, new Element("div")));
        detached.setId("capture-detached");
        add(new Paragraph("Element capture (expect: rejected:"
                + " IllegalArgumentException for both)"), attached, detached,
                capture);
    }

    private void tryCapture(Div result, Element element) {
        try {
            getUI().get().getPage()
                    .addDynamicImport(IMPORT_JS.withElement(element));
            result.setText("accepted");
        } catch (IllegalArgumentException ex) {
            result.setText("rejected: " + ex.getClass().getSimpleName() + ": "
                    + ex.getMessage());
        }
    }

    private static Div output(String id) {
        Div out = new Div();
        out.setId(id);
        out.getStyle().set("padding", "0.25em 0.5em")
                .set("border", "1px solid #ccc")
                .set("font-family", "monospace");
        return out;
    }
}
