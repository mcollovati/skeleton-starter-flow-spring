package org.vaadin.example;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.PlotOptionsColumn;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CspViolationException;

/**
 * Charts under {@code vaadin.csp=strict}, with flow-components PR #10714.
 * <p>
 * Charts sends its own JavaScript as declared calls, so ordinary charts work.
 * What does not work yet are the function properties, such as
 * {@code Tooltip.setFormatter(String)}: the browser compiles their source with
 * {@code eval}. With #10714 the server refuses them in strict mode. Each tab
 * shows one use case: format strings, updates from the server, a refused
 * function property, and a function declared in a {@link JsDefinition}
 * interface, which is part of the bundle.
 */
@Route("charts-csp")
public class ChartsCspView extends VerticalLayout {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ChartsCspView.class);

    /**
     * The script of {@link ChartFunctionsJs#applyDataLabelFormatter()}, a
     * constant so that the view can also show it.
     */
    static final String DATA_LABEL_FORMATTER_SCRIPT = """
            const apply = () => {
              if (this.configuration) {
                this.configuration.update({
                  plotOptions: { column: { dataLabels: {
                    formatter() { return this.y / 1000 + 'k'; }
                  } } }
                });
              }
            };
            this.addEventListener('chart-load', () => setTimeout(apply));
            apply();
            """;

    /**
     * Sets function options on the Highcharts instance of a chart, from the
     * bundle. Runs on the chart element. {@code this.configuration} is the
     * Highcharts chart once it is created, so the script also listens to
     * {@code chart-load}, which the chart fires when it is (re)created, and
     * applies the options right after it.
     */
    @JsDefinition
    interface ChartFunctionsJs extends Serializable {

        @JsExpression(DATA_LABEL_FORMATTER_SCRIPT)
        void applyDataLabelFormatter();
    }

    public ChartsCspView() {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Format strings", createFormatStringsTab());
        tabs.add("Server updates", createServerUpdatesTab());
        tabs.add("Function property", createFunctionPropertyTab());
        tabs.add("Declared function", createDeclaredFunctionTab());

        add(new H2("Charts with a strict Content Security Policy"),
                new Paragraph("""
                        The application runs with vaadin.csp=strict, so the \
                        browser refuses any JavaScript that it would have to \
                        compile from a string. Charts sends its own JavaScript \
                        from the bundle, so ordinary charts work. Function \
                        properties, such as Tooltip.setFormatter(String), are \
                        compiled with eval in the browser and do not work. \
                        Each tab shows one case."""),
                tabs);
    }

    /** Workaround 1: format strings, which are templates, not JavaScript. */
    private Component createFormatStringsTab() {
        return new VerticalLayout(
                new Paragraph("""
                        Workaround 1. A Highcharts format string is a \
                        template, not JavaScript, so a strict policy does not \
                        affect it. Highcharts 12 adds conditions and helpers, \
                        which replace many formatters."""),
                code("""
                        dataLabels.setFormat("{y} kg");
                        tooltip.setHeaderFormat("<b>{point.key}</b><br/>");
                        tooltip.setPointFormat("{series.name}: {point.y} kg");
                        tooltip.setFooterFormat("<br/>per crate");
                        legend.setLabelFormat("{name} series");
                        """),
                createBasicChart(),
                code("""
                        // Instead of labels.setFormatter(
                        //     "function() { return this.value / 1000 + 'k'; }")
                        yAxis.getLabels().setFormat("{divide value 1000}k");

                        // Instead of dataLabels.setFormatter(
                        //     "function() { return this.y > 5000 ? 'high' : 'low'; }")
                        dataLabels.setFormat("{#if (gt y 5000)}high{else}low{/if}");
                        """),
                createFormatStringChart(),
                code("""
                        yAxis.getLabels().setFormat("{value:,.2f}");
                        dataLabels.setFormat(
                                "{ucfirst key}: {multiply y 2}{#unless (gt y 5)} small{/unless}");
                        """),
                createHelpersChart());
    }

    /** Points added after the chart is drawn, sent as JSON updates. */
    private Component createServerUpdatesTab() {
        Span result = new Span("-");
        result.setId("add-point-result");
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("dynamic");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Points added from the server");
        DataSeries series = new DataSeries("Values");
        series.add(new DataSeriesItem("A", 1));
        conf.addSeries(series);
        conf.setPlotOptions(columnWithLabels("value {y}"));

        Button add = new Button("Add point", event -> {
            int next = series.size() + 1;
            series.add(new DataSeriesItem("P" + next, next * 10));
            result.setText("point added");
        });
        add.setId("add-point");

        return new VerticalLayout(new Paragraph("""
                Changes after the chart is drawn, such as a new point, are \
                sent as JSON to the chart, which applies them with its own \
                JavaScript. They work in strict mode."""),
                code("series.add(new DataSeriesItem(\"P2\", 20));"), add,
                result, chart);
    }

    /** The function property that is not migrated, refused on the server. */
    private Component createFunctionPropertyTab() {
        Span result = new Span("-");
        result.setId("string-formatter-result");

        Button button = new Button("Draw chart with a string formatter",
                event -> {
                    Chart chart = new Chart(ChartType.COLUMN);
                    Configuration conf = chart.getConfiguration();
                    conf.addSeries(new ListSeries("Values", 1, 2, 3));
                    conf.getTooltip().setFormatter(
                            "function() { return 'Value: ' + this.y; }");
                    try {
                        // Sends the configuration, which has a _fn_ property
                        chart.drawChart();
                        result.setText("drawn");
                    } catch (CspViolationException e) {
                        LOGGER.warn("Chart with a string formatter refused",
                                e);
                        result.setText("Refused on the server: "
                                + e.getClass().getSimpleName() + ": "
                                + e.getMessage());
                    }
                });
        button.setId("string-formatter");

        return new VerticalLayout(new Paragraph("""
                Not migrated. Tooltip.setFormatter(String) and the other \
                function properties send JavaScript source as a string, which \
                the browser would compile with eval. With flow-components \
                #10714, the server checks the configuration when it sends it, \
                and strict mode throws CspViolationException. The browser \
                never receives the chart, so its console stays empty: the \
                result appears below, and the stack trace in the server \
                log."""), code("""
                conf.getTooltip().setFormatter(
                        "function() { return 'Value: ' + this.y; }");
                // Throws CspViolationException in strict mode
                chart.drawChart();
                """), button, result);
    }

    /** Workaround 2: a function declared in a JsDefinition interface. */
    private Component createDeclaredFunctionTab() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("declared");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Data labels from a declared function");
        conf.getxAxis().setCategories("Q1", "Q2", "Q3");
        conf.addSeries(new ListSeries("Sales", 2000, 4000, 8000));
        PlotOptionsColumn plotOptions = new PlotOptionsColumn();
        plotOptions.setDataLabels(new DataLabels(true));
        conf.setPlotOptions(plotOptions);
        chart.addAttachListener(event -> chart.getElement()
                .executeJs(ChartFunctionsJs.class).applyDataLabelFormatter());

        return new VerticalLayout(new Paragraph("""
                Workaround 2, when a format string is not enough. The function \
                is declared in a @JsDefinition interface, so the build adds it \
                to the bundle and the browser does not compile it. It runs on \
                the chart element and sets the function on the Highcharts \
                chart. The function is not part of the configuration on the \
                server, so getFormatter() does not return it."""), code("""
                @JsDefinition
                interface ChartFunctionsJs extends Serializable {

                    @JsExpression(\"""
                """ + DATA_LABEL_FORMATTER_SCRIPT.indent(12) + """
                            \""")
                    void applyDataLabelFormatter();
                }

                chart.addAttachListener(event -> chart.getElement()
                        .executeJs(ChartFunctionsJs.class)
                        .applyDataLabelFormatter());
                """), chart);
    }

    private Chart createBasicChart() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("basic");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Fruit");
        conf.getxAxis().setCategories("Apples", "Pears", "Plums");
        conf.addSeries(new ListSeries("Weight", 12, 7, 3));
        conf.setPlotOptions(columnWithLabels("{y} kg"));
        Tooltip tooltip = conf.getTooltip();
        tooltip.setHeaderFormat("<b>{point.key}</b><br/>");
        tooltip.setPointFormat("{series.name}: {point.y} kg");
        tooltip.setFooterFormat("<br/>per crate");
        conf.getLegend().setLabelFormat("{name} series");
        return chart;
    }

    private Chart createFormatStringChart() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("format");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Sales");
        conf.getxAxis().setCategories("Q1", "Q2", "Q3");
        conf.getyAxis().getLabels().setFormat("{divide value 1000}k");
        conf.addSeries(new ListSeries("Sales", 3000, 6000, 9000));
        conf.setPlotOptions(columnWithLabels(
                "{#if (gt y 5000)}high{else}low{/if}"));
        return chart;
    }

    /**
     * Number formatting, and the other template helpers of Highcharts 12:
     * {@code ucfirst}, {@code multiply} and the {@code #unless} block.
     */
    private Chart createHelpersChart() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("helpers");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("More helpers");
        conf.getxAxis().setCategories("apples", "pears");
        conf.getyAxis().getLabels().setFormat("{value:,.2f}");
        conf.addSeries(new ListSeries("Count", 3, 8));
        conf.setPlotOptions(columnWithLabels(
                "{ucfirst key}: {multiply y 2}{#unless (gt y 5)} small{/unless}"));
        return chart;
    }

    private static PlotOptionsColumn columnWithLabels(String format) {
        DataLabels labels = new DataLabels(true);
        labels.setFormat(format);
        PlotOptionsColumn plotOptions = new PlotOptionsColumn();
        plotOptions.setDataLabels(labels);
        return plotOptions;
    }

    private static Pre code(String code) {
        Pre pre = new Pre(code);
        pre.getStyle().set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-s)");
        return pre;
    }
}
