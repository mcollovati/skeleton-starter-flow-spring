package org.vaadin.example.components;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.PlotOptionsColumn;
import com.vaadin.flow.component.charts.util.ChartSerialization;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

import tools.jackson.databind.node.ObjectNode;

/**
 * Prototype for option C of charts-declared-functions.md: a formatter declared
 * in a {@link JsDefinition} interface, placed inside the chart configuration as
 * the {@code @v-fn} object that Flow encodes a snippet as. Flow's client
 * decodes it into a function before {@code updateConfiguration} runs, so the
 * browser compiles nothing.
 * <p>
 * No flow-components change is used: the configuration node is built by hand
 * here, which is what a Jackson serializer in {@code ChartSerialization} would
 * do.
 */
@com.vaadin.flow.server.auth.AnonymousAllowed @Route("charts-declared-function")
public class ChartsDeclaredFunctionView extends VerticalLayout {

    /** What flow-components would add as the type of a function option. */
    public interface ChartFunction extends JsSnippet {
    }

    @JsDefinition
    public interface SalesFormatters extends JsSnippets {

        @JsExpression("return this.y / $0 + $1;")
        ChartFunction scaled(int divisor, String suffix);
    }

    private static final SalesFormatters FORMATTERS = JsSnippets
            .of(SalesFormatters.class);

    public ChartsDeclaredFunctionView() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("declared-config");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Data labels from a declared function in the configuration");
        conf.getxAxis().setCategories("Q1", "Q2", "Q3");
        conf.addSeries(new ListSeries("Sales", 2000, 4000, 8000));
        PlotOptionsColumn plotOptions = new PlotOptionsColumn();
        plotOptions.setDataLabels(new DataLabels(true));
        conf.setPlotOptions(plotOptions);

        chart.addAttachListener(event -> {
            // Serialize the configuration as Chart.drawChart does, then put
            // the encoded snippet where a serializer for ChartFunction would
            // put it: under the plain option name, not _fn_formatter
            ObjectNode node = JacksonUtils
                    .readTree(ChartSerialization.toJSON(conf));
            ObjectNode dataLabels = (ObjectNode) node.get("plotOptions")
                    .get("column").get("dataLabels");
            dataLabels.set("formatter",
                    JacksonCodec.encodeWithTypeInfo(FORMATTERS.scaled(1000, "k")));
            chart.getElement().callJsFunction("updateConfiguration", node,
                    false);
        });

        add(new H2("Declared function inside the configuration"),
                new Paragraph("""
                        The formatter is declared in a @JsDefinition \
                        interface and sent inside the configuration as a \
                        reference to the bundled function. Expected data \
                        labels: 2k, 4k, 8k."""),
                chart);
    }
}
