package org.vaadin.example.ecosystem;

import java.util.List;

import com.flowingcode.vaadin.addons.orgchart.OrgChart;
import com.flowingcode.vaadin.addons.orgchart.OrgChartItem;
import com.vaadin.flow.router.Route;

/**
 * OrgChartAddon (com.flowingcode.vaadin.addons:orgchart-addon 5.4.0). It
 * initializes the chart with a string in onAttach.
 */
@Route("eco/orgchart")
public class OrgChartView extends EcoView {

    public OrgChartView() {
        super("OrgChartAddon");
        OrgChartItem root = new OrgChartItem(1, "John Williams", "Director");
        root.setChildren(List.of(new OrgChartItem(2, "Anna Thompson", "Manager")));
        OrgChart chart = withId(new OrgChart(root), "chart");
        add(chart);
        add(button("add", "Add", () -> chart.addChildren(2,
                List.of(new OrgChartItem(3, "Mark Brown", "Developer")))));
    }
}
