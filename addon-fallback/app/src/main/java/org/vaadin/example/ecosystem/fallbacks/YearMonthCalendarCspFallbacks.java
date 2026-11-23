package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of YearMonthCalendarAddon
 * (org.vaadin.addons.flowingcode:year-month-calendar 4.6.2). Each declaration
 * was pasted from the message that Flow printed for the refused call; only
 * the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface YearMonthCalendarCspFallbacks {

    @JsExpression("this.month=new Date($0,$1-1,1);")
    PendingJavaScriptResult setMonth(Object arg0, Object arg1);

    @JsExpression("setTimeout(()=>{this._setStyleForDay($0,$1);})")
    PendingJavaScriptResult setStyleForDay(Object arg0, Object arg1);

    @JsExpression("setTimeout(()=>this._clearEmptyDaysStyle())")
    PendingJavaScriptResult clearEmptyDaysStyle();
}
