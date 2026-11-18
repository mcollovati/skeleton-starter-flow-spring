package org.vaadin.example;

import java.io.Serializable;

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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
 * {@code eval}. With #10714 the server refuses them in strict mode. The view
 * shows that case, and two workarounds: a Highcharts format string, and a
 * function declared in a {@link JsDefinition} interface, which is part of the
 * bundle.
 */
@Route("charts-csp")
public class ChartsCspView extends VerticalLayout {

    /**
     * Sets function options on the Highcharts instance of a chart, from the
     * bundle. Runs on the chart element. {@code this.configuration} is the
     * Highcharts chart once it is created, so the script also listens to
     * {@code chart-load}, which the chart fires when it is (re)created, and
     * applies the options right after it.
     */
    @JsDefinition
    interface ChartFunctionsJs extends Serializable {

        @JsExpression("""
                const apply = () => {
                  if (!this.configuration) {
                    return;
                  }
                  this.configuration.update({
                    plotOptions: {
                      column: {
                        dataLabels: {
                          formatter() {
                            return this.y / 1000 + 'k*';
                          }
                        }
                      }
                    }
                  });
                };
                // chart-load fires before the chart is assigned to configuration
                this.addEventListener('chart-load', () => setTimeout(apply));
                apply();
                """)
        void applyDataLabelFormatter();
    }

    public ChartsCspView() {
        Span log = new Span("-");
        log.setId("log");

        add(new H3("Column chart with data label format"),
                createBasicChart(),
                new H3("Format strings with template helpers"),
                new Paragraph(
                        "Workaround 1: a Highcharts format string needs no JavaScript."),
                createFormatStringChart(),
                new H3("Number formatting and more helpers"),
                createHelpersChart(), new H3("Updates from the server"),
                createDynamicChart(log),
                new H3("Function property (not migrated)"),
                new Paragraph(
                        "Tooltip.setFormatter(String) sends a function as a string. "
                                + "Strict mode refuses it on the server."),
                createStringFunctionButton(log),
                new H3("Declared function"),
                new Paragraph(
                        "Workaround 2: the function is declared in a @JsDefinition "
                                + "interface and set on the Highcharts instance."),
                createDeclaredFunctionChart(), new Paragraph("Log:"), log);
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

    private Chart createFormatStringChart() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("format");
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Sales");
        conf.getxAxis().setCategories("Q1", "Q2", "Q3");
        // Instead of a formatter that divides the value by 1000
        conf.getyAxis().getLabels().setFormat("{divide value 1000}k");
        conf.addSeries(new ListSeries("Sales", 3000, 6000, 9000));
        // Instead of a formatter with an if
        conf.setPlotOptions(columnWithLabels(
                "{#if (gt y 5000)}high{else}low{/if}"));
        return chart;
    }

    private VerticalLayout createDynamicChart(Span log) {
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
            log.setText("point added");
        });
        add.setId("add-point");
        return new VerticalLayout(chart, add);
    }

    private Button createStringFunctionButton(Span log) {
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
                        add(chart);
                        log.setText("drawn");
                    } catch (CspViolationException e) {
                        log.setText("refused: " + e.getClass().getSimpleName());
                    }
                });
        button.setId("string-formatter");
        return button;
    }

    private Chart createDeclaredFunctionChart() {
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
        return chart;
    }

    private static PlotOptionsColumn columnWithLabels(String format) {
        DataLabels labels = new DataLabels(true);
        labels.setFormat(format);
        PlotOptionsColumn plotOptions = new PlotOptionsColumn();
        plotOptions.setDataLabels(labels);
        return plotOptions;
    }
}
