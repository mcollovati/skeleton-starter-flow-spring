package org.vaadin.example.csp;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.example.CspView;
import org.vaadin.example.csp.CspViolationCollector.Owner;
import org.vaadin.example.csp.CspViolationCollector.Violation;

/**
 * Runs the view in the warn mode and checks which of its call sites would
 * break under a strict CSP.
 */
@SpringBootTest(properties = "vaadin.csp=warn")
class CspViewCspTest extends SpringBrowserlessTest {

    @RegisterExtension
    static CspViolationCollector csp = new CspViolationCollector();

    @Test
    void openingTheViewReportsConstructorAndAttachCode() {
        navigate(CspView.class);

        // addEventData and setFilter in the constructor, executeJs(String)
        // in onAttach. executeJs(TitleJs.class) is not reported.
        Assertions.assertEquals(List.of(
                "CspView.<init>(CspView.java:30)",
                "CspView.<init>(CspView.java:31)",
                "CspView.onAttach(CspView.java:39)"), appCallSites());
    }

    @Test
    void clickingReportsTheListenerCode() {
        navigate(CspView.class);
        test($(Button.class).id("log")).click();

        // Code in a listener is found only if a test runs it
        Assertions.assertTrue(appCallSites().stream()
                .anyMatch(s -> s.startsWith("CspView.lambda$new$")),
                appCallSites().toString());
    }

    private List<String> appCallSites() {
        return csp.currentViolations().stream()
                .filter(v -> v.owner() == Owner.APP)
                .map(Violation::callSite).distinct().toList();
    }
}
