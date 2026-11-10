package org.vaadin.example.review;

import java.io.Serializable;
import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Finding 4 (in a touched function, also on main): the keydown delegate is
 * registered once, with the options the shortcut has at that time. Changing an
 * option of an active shortcut (setBrowserDefaultAllowed,
 * setResetFocusOnActiveElement, setEventsFromNestedModalsAllowed) updates only
 * the listener filter. The delegate in the browser keeps calling
 * preventDefault() on the original keydown, so the browser default stays
 * blocked for a shortcut that listens on a browser-only element.
 * <p>
 * Expected: a delegate with allowDefault=true is scheduled after the change.
 * Actual: nothing is scheduled.
 */
@SpringBootTest
class DelegateOptionsTest extends SpringBrowserlessTest {

    @JsDefinition
    public interface LocatorJs extends Serializable {

        @JsExpression("return document.getElementById($0);")
        JsFunction byId(String id);
    }

    @Test
    void browserDefaultAllowedLater_delegateFollows() {
        UI ui = UI.getCurrent();
        Div host = new Div();
        ui.add(host);
        Shortcuts.setShortcutListenOnElement(
                JsSnippets.of(LocatorJs.class).byId("overlay"), host);
        ShortcutRegistration shortcut = Shortcuts.addShortcutListener(host,
                () -> {
                }, Key.KEY_D, KeyModifier.ALT).listenOn(host);

        List<JsCall> first = delegateCalls(ui);
        assertEquals(1, first.size());
        assertEquals(false, first.get(0).arguments().get(5), "allowDefault");

        shortcut.setBrowserDefaultAllowed(true);
        List<JsCall> second = delegateCalls(ui);
        assertEquals(1, second.size(),
                "the delegate in the browser still prevents the default");
        assertEquals(true, second.get(0).arguments().get(5), "allowDefault");
    }

    private static List<JsCall> delegateCalls(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(inv -> inv.getInvocation().getJsCall())
                .filter(call -> call != null && call.definitionType()
                        .getSimpleName().equals("ShortcutJs"))
                .toList();
    }
}
