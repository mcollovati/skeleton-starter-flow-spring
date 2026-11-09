package org.vaadin.example;

import java.io.Serializable;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * Methods with typed results (vaadin/flow#25811, PR #26890). Each method is
 * used by one row of {@link TypedResultView}.
 */
@JsDefinition
public interface TypedResultJs extends Serializable {

    // Conversion to simple types, beans and generic types

    @JsExpression("return 42")
    JavaScriptResult<Integer> answer();

    @JsExpression("return {name: 'Ada', age: 36}")
    JavaScriptResult<Person> person();

    @JsExpression("return [{name: 'Ada', age: 36}, {name: 'Alan', age: 41}]")
    JavaScriptResult<List<Person>> people();

    @JsExpression("return new Promise(resolve => setTimeout(() => resolve($0 * 2), 500))")
    JavaScriptResult<Integer> doubleLater(int value);

    // How a JavaScript null arrives

    @JsExpression("return null")
    JavaScriptResult<JsonNode> nullAsJsonNode();

    @JsExpression("return null")
    JavaScriptResult<ObjectNode> nullAsObjectNode();

    @JsExpression("return null")
    JavaScriptResult<Integer> nullAsInteger();

    @JsExpression("return null")
    PendingJavaScriptResult nullAsPending();

    // Failures

    @JsExpression("return 'abc'")
    JavaScriptResult<Integer> notANumber();

    @JsExpression("return [1, 2]")
    JavaScriptResult<ObjectNode> arrayAsObject();

    @JsExpression("throw new Error('boom')")
    JavaScriptResult<Integer> fails();

    // Released API, for comparison

    @JsExpression("return 'abc'")
    PendingJavaScriptResult notANumberPending();
}
