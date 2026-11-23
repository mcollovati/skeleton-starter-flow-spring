package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.markdown.MarkdownViewer;
import com.vaadin.flow.router.Route;

/**
 * MarkdownEditor (com.flowingcode.vaadin.addons:markdown-editor-addon 2.0.3):
 * the viewer sends one string before the client response after attach and
 * one in onDetach.
 */
@Route("eco/markdown")
public class MarkdownViewerView extends EcoView {

    public MarkdownViewerView() {
        super("MarkdownEditor");
        MarkdownViewer viewer = withId(new MarkdownViewer("# Hello CSP"),
                "viewer");
        add(viewer);
        var removed = output("removed");
        add(button("remove", "Remove", () -> {
            remove(viewer);
            removed.setText("removed");
        }), removed);
    }
}
