package com.acme.addon;

import com.vaadin.flow.component.UI;

/**
 * Case 3: {@code Page.executeJs(String, Object...)}, a call on the page and not
 * on an element (no {@code this}), with values passed as parameters.
 */
public final class PageTitle {

    private PageTitle() {
    }

    public static void setWithBadge(UI ui, String title, int count) {
        ui.getPage().executeJs("document.title = $0 + ' (' + $1 + ')'", title,
                count);
    }
}
