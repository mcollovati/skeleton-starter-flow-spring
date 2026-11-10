package org.vaadin.example.review;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Finding 1: browserless-test (TestBench UI unit testing) fireShortcut finds
 * ShortcutRegistration.generateEventKeyFilter and generateEventModifierFilter
 * by reflection, and matches the filter as a string that starts with "([".
 * The branch removes both methods and the string filter.
 * <p>
 * main: passes. Branch: fails with ExceptionInInitializerError /
 * NoSuchMethodException from MockFilterJsonObject.
 */
@SpringBootTest
class FireShortcutTest extends SpringBrowserlessTest {

    @Test
    void fireShortcut_runsShortcutListener() {
        AtomicInteger fired = new AtomicInteger();
        UI.getCurrent().addShortcutListener(fired::incrementAndGet, Key.KEY_S,
                KeyModifier.CONTROL);
        fireShortcut(Key.KEY_S, KeyModifier.CONTROL);
        assertEquals(1, fired.get());
    }
}
