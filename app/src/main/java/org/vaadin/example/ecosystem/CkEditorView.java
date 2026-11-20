package org.vaadin.example.ecosystem;

import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * vaadin-ckeditor (com.wontlost:ckeditor-vaadin 6.1.0).
 */
@Route("eco/ckeditor")
public class CkEditorView extends EcoView {

    public CkEditorView() {
        super("vaadin-ckeditor");
        VaadinCKEditor editor = withId(
                VaadinCKEditor.withPreset(CKEditorPreset.BASIC), "editor");
        add(editor);
        add(button("set-value", "Set value",
                () -> editor.setValue("<p>Hello CSP</p>")));
        add(button("read-only", "Read only", () -> editor.setReadOnly(true)));
        add(button("insert", "Insert", () -> editor.insertText(" world")));
    }
}
