package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of super-fields
 * (pl.unforgiven:superfields 0.21.2). Each declaration was pasted from the
 * message that Flow printed for the refused call; only the method names were
 * changed.
 */
@JsDefinition
@JsStringFallback
public interface SuperFieldsCspFallbacks extends JsInvokers {

    @JsExpression("if(this.inputElement !== undefined) {this.inputElement.inputMode = $0;}")
    PendingJavaScriptResult setTextInputMode(Object arg0);

    @JsExpression("this.$.table.addEventListener('click', function(e) {this.preventSelection && e.stopPropagation()}.bind(this)); this.$.table.addEventListener('keydown', function(e){this.preventSelection && e.keyCode === 32 && e.stopPropagation()}.bind(this));")
    PendingJavaScriptResult preventGridSelection();

    @JsExpression("$0.addEventListener('selected-items-changed', (e) => {const {target: grid} = e;if (grid.selectedItems.length > grid.maximumSelectionSize && grid.maximumSelectionSize > 0) {console.log('GMS: preventing selection, limit reached'); console.log('GMS: selected '+grid.selectedItems.length+' - limit '+grid.maximumSelectionSize); e.stopPropagation();grid.splice('selectedItems', -1);console.log('GMS: selected items are '+JSON.stringify(grid.selectedItems)); }});")
    PendingJavaScriptResult gridMultiSelectLimit();
}
