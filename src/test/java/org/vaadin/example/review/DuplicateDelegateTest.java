package org.vaadin.example.review;

import java.io.Serializable;
import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Finding 3: the branch tells keydown delegates apart by the JsCall of a
 * declared locator (a record). A record compares a variadic or array argument
 * by identity, so the same locator call set again is a "new" locator, and a
 * second delegate is registered on the client. Each keydown is then relayed
 * twice and every shortcut on the listen-on component fires twice.
 * <p>
 * The single-String locator gets one delegate (control case). The variadic
 * locator gets two. Expected: one in both cases.
 */
@SpringBootTest
class DuplicateDelegateTest extends SpringBrowserlessTest {

    @JsDefinition
    public interface LocatorJs extends Serializable {

        @JsExpression("return document.getElementById($0);")
        JsFunction byId(String id);

        @JsExpression("return document.querySelector($0.map((id) => '#' + id).join(','));")
        JsFunction byAnyId(String... ids);
    }

    private static final LocatorJs JS = JsSnippets.of(LocatorJs.class);

    @Test
    void sameSingleArgumentLocatorSetTwice_oneDelegate() {
        assertEquals(1, registerTwiceAndCountDelegates(() -> JS.byId("overlay")));
    }

    @Test
    void sameVariadicLocatorSetTwice_oneDelegate() {
        assertEquals(1,
                registerTwiceAndCountDelegates(() -> JS.byAnyId("overlay")));
    }

    /**
     * The same function body given with JsFunction.of: compared by identity,
     * so also two delegates, while the same body as a deprecated String
     * locator got one (main and branch).
     */
    @Test
    void sameBodyLocatorSetTwice_oneDelegate() {
        assertEquals(1, registerTwiceAndCountDelegates(() -> JsFunction
                .of("return document.getElementById('overlay');")));
    }

    private static long registerTwiceAndCountDelegates(
            java.util.function.Supplier<JsFunction> locator) {
        UI ui = UI.getCurrent();
        Div host = new Div();
        ui.add(host);
        long delegates = 0;
        // As a component does before each listenOn: set the locator again.
        // The second shortcut is added in a later round trip, e.g. when a
        // dialog adds a shortcut on a button click.
        for (int i = 0; i < 2; i++) {
            Shortcuts.setShortcutListenOnElementLocator(locator.get(), host);
            Shortcuts.addShortcutListener(host, () -> {
            }, Key.KEY_D, KeyModifier.ALT).listenOn(host);
            ui.getInternals().getStateTree()
                    .runExecutionsBeforeClientResponse();
            List<PendingJavaScriptInvocation> invocations = ui.getInternals()
                    .dumpPendingJavaScriptInvocations();
            delegates += invocations.stream()
                    .map(inv -> inv.getInvocation().getJsCall())
                    .filter(call -> call != null && call.definitionType()
                            .getSimpleName().equals("ShortcutJs"))
                    .map(JsCall::methodName).count();
        }
        return delegates;
    }
}
