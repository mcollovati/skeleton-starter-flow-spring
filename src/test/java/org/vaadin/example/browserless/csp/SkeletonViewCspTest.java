package org.vaadin.example.browserless.csp;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.example.browserless.SkeletonView;
import org.vaadin.example.browserless.csp.CspViolationCollector.Owner;

/**
 * The skeleton view uses no string JavaScript itself, but Flow (UI, Page) and
 * Button do. Those are reported as VAADIN.
 */
@SpringBootTest(properties = "vaadin.csp=warn")
class SkeletonViewCspTest extends SpringBrowserlessTest {

    @RegisterExtension
    static CspViolationCollector csp = new CspViolationCollector();

    @Test
    void skeletonViewHasNoApplicationViolations() {
        navigate(SkeletonView.class);
        test($(Button.class).single()).click();
        Assertions.assertEquals(1, $(Paragraph.class).all().size());

        Assertions.assertTrue(csp.currentViolations().stream()
                .noneMatch(v -> v.owner() == Owner.APP));
        Assertions.assertTrue(csp.currentViolations().stream()
                .anyMatch(v -> v.owner() == Owner.VAADIN));
    }
}
