package org.vaadin.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * Runs every typed result case of {@link TypedResultJs} and the inherited
 * definitions in {@link InheritedJs}. Each row shows the expected result and,
 * after "Run all", the actual one.
 */
@Route("")
public class TypedResultView extends VerticalLayout {

    private final Map<String, Span> actuals = new HashMap<>();

    public TypedResultView() {
        Button run = new Button("Run all", event -> runAll());
        run.setId("run");
        add(run);

        section("Conversion");
        row("answer", "JavaScriptResult<Integer>, returns 42", "42 (Integer)");
        row("person", "JavaScriptResult<Person>, returns an object",
                "Person[name=Ada, age=36]");
        row("people", "JavaScriptResult<List<Person>>, returns an array",
                "[Person[name=Ada, age=36], Person[name=Alan, age=41]],"
                        + " elements are Person");
        row("promise",
                "JavaScriptResult<Integer>, returns a Promise,"
                        + " toCompletableFuture()",
                "42");

        section("JavaScript null");
        row("null-jsonnode", "JavaScriptResult<JsonNode>", "NullNode");
        row("null-objectnode", "JavaScriptResult<ObjectNode>", "Java null");
        row("null-integer", "JavaScriptResult<Integer>", "Java null");
        row("null-pending", "PendingJavaScriptResult, then(Consumer<JsonNode>)",
                "NullNode");

        section("Failures");
        row("convert-error-handler",
                "JavaScriptResult<Integer> returns 'abc', with error handler",
                "error handler: Cannot deserialize JSON to type"
                        + " java.lang.Integer ...");
        row("convert-no-handler",
                "JavaScriptResult<Integer> returns 'abc', then(handler) without"
                        + " error handler, then toCompletableFuture() on the"
                        + " same result",
                "result handler not called, a WARN in the server log,"
                        + " future failed with JavaScriptException");
        row("wrong-node",
                "JavaScriptResult<ObjectNode> returns an array, with error"
                        + " handler",
                "error handler: Cannot decode JSON of type ARRAY as"
                        + " ...ObjectNode");
        row("throws", "JavaScriptResult<Integer> throws, with error handler",
                "error handler: Error: boom");
        row("throws-future",
                "JavaScriptResult<Integer> throws, toCompletableFuture()",
                "future failed with JavaScriptException: Error: boom");

        section("Inherited from the generic ValueJs<T>");
        row("inherited-int", "IntValueJs extends ValueJs<Integer>, parse(\"7\")",
                "7 (Integer)");
        row("inherited-list",
                "PeopleValueJs extends ValueJs<List<Person>>",
                "[Person[name=Ada, age=36]], elements are Person");
        row("inherited-raw", "RawValueJs extends ValueJs (raw)",
                "refused: ... returning JavaScriptResult without a type to"
                        + " convert the result to ...");

        section("Released API, for comparison (separate button)");
        Button runPending = new Button("Run PendingJavaScriptResult case",
                event -> runPendingFuture());
        runPending.setId("run-pending");
        add(runPending);
        row("pending-future",
                "PendingJavaScriptResult returns 'abc',"
                        + " toCompletableFuture(Integer.class)",
                "future failed with JavaScriptException. Today (released"
                        + " behavior) the future never completes and the"
                        + " exception goes to the server log");
    }

    private void runPendingFuture() {
        show("pending-future", "future still pending");
        getElement().executeJs(TypedResultJs.class).notANumberPending()
                .toCompletableFuture(Integer.class)
                .whenComplete((value, error) -> show("pending-future",
                        error == null ? "future completed with " + value
                                : "future failed with "
                                        + error.getClass().getSimpleName()
                                        + ": " + error.getMessage()));
    }

    private void runAll() {
        Element element = getElement();
        TypedResultJs js = element.executeJs(TypedResultJs.class);

        js.answer().then(value -> show("answer", value + " ("
                + value.getClass().getSimpleName() + ")"));
        js.person().then(value -> show("person", String.valueOf(value)));
        js.people().then(value -> show("people", describeList(value)));
        js.doubleLater(21).toCompletableFuture()
                .whenComplete((value, error) -> show("promise",
                        error == null ? String.valueOf(value)
                                : "failed: " + error));

        js.nullAsJsonNode().then(value -> show("null-jsonnode", describe(value)));
        js.nullAsObjectNode()
                .then(value -> show("null-objectnode", describe(value)));
        js.nullAsInteger().then(value -> show("null-integer", describe(value)));
        js.nullAsPending().then(value -> show("null-pending", describe(value)));

        js.notANumber().then(
                value -> show("convert-error-handler",
                        "result handler called with " + value),
                error -> show("convert-error-handler",
                        "error handler: " + error));

        show("convert-no-handler", "result handler not called");
        JavaScriptResult<Integer> notANumber = js.notANumber();
        notANumber.then(value -> show("convert-no-handler",
                "result handler called with " + value));
        notANumber.toCompletableFuture()
                .whenComplete((value, error) -> append("convert-no-handler",
                        error == null ? ", future completed with " + value
                                : ", future failed with "
                                        + error.getClass().getSimpleName()));

        js.arrayAsObject().then(
                value -> show("wrong-node",
                        "result handler called with " + describe(value)),
                error -> show("wrong-node", "error handler: " + error));
        js.fails().then(
                value -> show("throws", "result handler called with " + value),
                error -> show("throws", "error handler: " + error));
        js.fails().toCompletableFuture()
                .whenComplete((value, error) -> show("throws-future",
                        error == null ? "future completed with " + value
                                : "future failed with "
                                        + error.getClass().getSimpleName()
                                        + ": " + error.getMessage()));

        element.executeJs(InheritedJs.IntValueJs.class).parse("7")
                .then(value -> show("inherited-int", value + " ("
                        + value.getClass().getSimpleName() + ")"));
        element.executeJs(InheritedJs.PeopleValueJs.class)
                .parse("[{\"name\": \"Ada\", \"age\": 36}]")
                .then(value -> show("inherited-list", describeList(value)));
        try {
            element.executeJs(InheritedJs.RawValueJs.class);
            show("inherited-raw", "accepted");
        } catch (IllegalArgumentException e) {
            show("inherited-raw", "refused: " + e.getMessage());
        }
    }

    private static String describe(Object value) {
        return value == null ? "Java null" : value.getClass().getSimpleName();
    }

    private static String describeList(List<?> list) {
        return list + ", elements are " + list.stream()
                .map(element -> element.getClass().getSimpleName()).distinct()
                .toList();
    }

    private void section(String title) {
        add(new H3(title));
    }

    private void row(String id, String description, String expected) {
        Span actual = new Span("(not run)");
        actual.setId(id);
        actuals.put(id, actual);
        Div row = new Div(new Div(description),
                new Div("Expected: " + expected), new Div(new Span("Actual: "),
                        actual));
        row.getStyle().set("margin-bottom", "0.75em");
        add(row);
    }

    private void show(String id, String text) {
        actual(id).setText(text);
    }

    private void append(String id, String text) {
        Span span = actual(id);
        span.setText(span.getText() + text);
    }

    private Span actual(String id) {
        return actuals.get(id);
    }
}
