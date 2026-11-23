package org.vaadin.example.browserless.csp;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.example.browserless.SkeletonView;

/**
 * The skeleton view uses no string JavaScript. On 25.4.0-alpha5 Flow (UI,
 * Page) and Button did (reported as VAADIN); on Flow main they no longer do.
 */
@SpringBootTest(properties = "vaadin.csp=warn")
class SkeletonViewCspTest extends SpringBrowserlessTest {

    @RegisterExtension
    static CspViolationCollector csp = new CspViolationCollector();

    @Test
    void skeletonViewHasNoViolations() {
        navigate(SkeletonView.class);
        test($(Button.class).single()).click();
        Assertions.assertEquals(1, $(Paragraph.class).all().size());

        Assertions.assertEquals(List.of(), csp.currentViolations());
    }
}
