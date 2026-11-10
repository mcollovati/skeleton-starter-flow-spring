package org.vaadin.example;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.JsLitItemTemplate;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CspViolationException;

/**
 * Grid and TreeGrid features under {@code vaadin.csp=strict}, with the
 * prototype of declared Lit templates (flow-components branch
 * prototype/grid-lit-template-csp, Flow PR vaadin/flow#26960).
 * <p>
 * Every column renders with JavaScript from the bundle, so the browser
 * compiles nothing. The last button adds a column with a string template,
 * which strict mode refuses on the server.
 */
@Route("grid-csp")
public class GridCspView extends VerticalLayout {

    public record Person(String name, int age, LocalDate birthday,
            String status) {
    }

    /** An application template, declared like the built-in ones. */
    @JsDefinition
    public interface PersonTemplates extends Serializable {

        /**
         * A badge with the status of the person and a capture ($0) for the
         * label, which calls the server with {@code functions.select}.
         */
        @JsExpression("""
                return html`<span class="status" theme="badge"
                    @click=${functions.select}>${$0}: ${item.status}</span>`;
                """)
        JsLitItemTemplate statusBadge(String label);
    }

    private static final List<Person> PEOPLE = List.of(
            new Person("Ada", 36, LocalDate.of(1990, 1, 10), "active"),
            new Person("Linus", 28, LocalDate.of(1998, 5, 2), "away"),
            new Person("Grace", 45, LocalDate.of(1981, 12, 9), "active"));

    public GridCspView() {
        Span log = new Span("-");
        log.setId("log");

        add(new H3("Grid"), createGrid(log), new H3("TreeGrid"),
                createTreeGrid(), new H3("Component hierarchy column"),
                createComponentTreeGrid(), new H3("String template"),
                new Paragraph("Strict mode should refuse this on the server."),
                createStringTemplateButton(log), new Paragraph("Log:"), log);
    }

    private Grid<Person> createGrid(Span log) {
        Grid<Person> grid = new Grid<>();
        grid.setId("grid");
        grid.setItems(PEOPLE);
        grid.setAllRowsVisible(true);

        grid.addColumn(Person::name).setHeader("Path");
        grid.addComponentColumn(person -> new Span("C:" + person.name()))
                .setHeader("Component");
        grid.addColumn(new TextRenderer<>(person -> "T:" + person.name()))
                .setHeader("Text");
        grid.addColumn(new NumberRenderer<>(Person::age, "%d years"))
                .setHeader("Number");
        grid.addColumn(new LocalDateRenderer<>(Person::birthday, "yyyy-MM-dd"))
                .setHeader("Date");
        grid.addColumn(new NativeButtonRenderer<>("Click",
                person -> log.setText("button " + person.name())))
                .setHeader("Button");
        grid.addColumn(LitRenderer
                .<Person> of(JsSnippets.of(PersonTemplates.class)
                        .statusBadge("Status"))
                .withProperty("status", Person::status)
                .withFunction("select",
                        person -> log.setText("select " + person.name())))
                .setHeader("App template");

        grid.setItemDetailsRenderer(new ComponentRenderer<Span, Person>(
                person -> new Span("Details of " + person.name())));
        return grid;
    }

    private TreeGrid<String> createTreeGrid() {
        TreeGrid<String> treeGrid = new TreeGrid<>();
        treeGrid.setId("tree-grid");
        treeGrid.setItems(List.of("Parent 1", "Parent 2"),
                item -> !item.contains("/")
                        ? List.of(item + " / Child 1", item + " / Child 2")
                        : List.of());
        treeGrid.addHierarchyColumn(item -> item).setHeader("Hierarchy");
        treeGrid.setAllRowsVisible(true);
        return treeGrid;
    }

    private TreeGrid<String> createComponentTreeGrid() {
        TreeGrid<String> treeGrid = new TreeGrid<>();
        treeGrid.setId("component-tree-grid");
        treeGrid.setItems(List.of("Folder"),
                item -> item.equals("Folder") ? List.of("File") : List.of());
        treeGrid.addComponentHierarchyColumn(item -> new Span("[" + item + "]"))
                .setHeader("Component hierarchy");
        treeGrid.setAllRowsVisible(true);
        return treeGrid;
    }

    private Button createStringTemplateButton(Span log) {
        Button button = new Button("Add string template grid", event -> {
            Grid<Person> grid = new Grid<>();
            grid.setItems(PEOPLE);
            grid.addColumn(LitRenderer.<Person> of("<b>${item.name}</b>")
                    .withProperty("name", Person::name));
            try {
                add(grid);
            } catch (CspViolationException e) {
                remove(grid);
                log.setText("refused: " + e.getClass().getSimpleName());
            }
        });
        button.setId("string-template");
        return button;
    }
}
