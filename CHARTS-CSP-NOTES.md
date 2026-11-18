# Charts under `vaadin.csp=strict`

The `/charts-csp` view (`ChartsCspView`) runs Charts with a strict Content
Security Policy: `script-src 'nonce-…' 'strict-dynamic'`, without
`unsafe-eval`.

## Background

Charts sends its own JavaScript as declared calls (`ChartJs`), so ordinary
charts work in strict mode. What does not work yet are the **function
properties**: setters such as `Tooltip.setFormatter(String)` take the source
code of a JavaScript function. The configuration goes to the browser as JSON
with `_fn_` keys, and the web component turns each of them into a function with
`eval` (`inflateFunctions` in `@vaadin/charts/src/helpers.js`). A strict CSP
blocks that `eval`.

flow-components PR vaadin/flow-components#10714 reports such a configuration
on the server when it is sent: nothing when CSP is off, a warning in the warn
mode, and a `CspViolationException` in the strict mode. Without it, the chart
fails only in the browser, with an `unsafe-eval` violation and nothing on the
server.

## What it needs

- flow-components branch `feat/charts-function-csp-report` (PR #10714),
  installed as 25.4-SNAPSHOT: `vaadin-charts-flow` (built with `-am`).
- The other 25.4-SNAPSHOT artifacts from the local repository, as for the
  Grid view.
- A Vaadin pro key, because Charts is a commercial component.

Build offline (`-o`), so Maven does not replace the local snapshots with
remote ones.

## What the view covers

| Section | API | Result in strict mode |
|---|---|---|
| Column chart | `DataLabels.setFormat("{y} kg")`; tooltip `setHeaderFormat`, `setPointFormat`, `setFooterFormat`; `Legend.setLabelFormat("{name} series")` | works |
| Format strings | `{divide value 1000}k` on the y axis labels, `{#if (gt y 5000)}high{else}low{/if}` on the data labels | works |
| Number formatting and more helpers | y axis labels `{value:,.2f}`, data labels `{ucfirst key}: {multiply y 2}{#unless (gt y 5)} small{/unless}` | works |
| Updates from the server | `DataSeries.add(...)` after the chart is drawn (point update path) | works |
| Function property, not migrated | `Tooltip.setFormatter("function() {...}")` | refused on the server with `CspViolationException` (#10714) |
| Workaround 1 | Highcharts format strings, see "Format strings" | works |
| Workaround 2 | the function is declared in a `@JsDefinition` interface and set on the Highcharts instance with `chart.update(...)` | works |

## The workarounds

**1. Format strings (preferred).** Highcharts format strings are templates, not
JavaScript, so a strict CSP does not affect them. The model has them next to
most formatters: `setFormat`, `setPointFormat`, `setHeaderFormat`,
`setFooterFormat`, `setLabelFormat`. Highcharts 12.2.0 is bundled with Vaadin 25.4 (`/charts` pins it). Its
template helpers, from `highcharts/es-modules/Core/Templating.js`, are `add`,
`subtract`, `multiply`, `divide`, `eq`, `ne`, `gt`, `ge`, `lt`, `le`, `#if`,
`#unless`, `#each` and `ucfirst`; templating uses no `eval`. So many
formatters can be replaced:

| Formatter | Format string |
|---|---|
| `function() { return this.value / 1000 + 'k'; }` | `{divide value 1000}k` |
| `function() { return this.y > 5000 ? 'high' : 'low'; }` | `{#if (gt y 5000)}high{else}low{/if}` |
| `function() { return this.series.name + ': ' + this.y; }` | `{series.name}: {y}` |
| number formatting | `{value:,.2f}` |

**2. A declared function.** When a format string is not enough, write the
function in a `@JsDefinition` interface, so it is part of the bundle, and set
it on the Highcharts instance from the browser:

```java
@JsDefinition
interface ChartFunctionsJs extends Serializable {

    @JsExpression("""
            const apply = () => this.configuration.update({
              plotOptions: { column: { dataLabels: {
                formatter() { return this.y / 1000 + 'k*'; } } } }
            });
            this.addEventListener('chart-load', apply);
            if (this.configuration) {
              apply();
            }
            """)
    void applyDataLabelFormatter();
}

chart.addAttachListener(event -> chart.getElement()
        .executeJs(ChartFunctionsJs.class).applyDataLabelFormatter());
```

`this.configuration` is the Highcharts chart once it is created. The script
also listens to `chart-load`, which the chart fires when it is created again,
for example after `drawChart(true)`, because a new chart does not have the
function. Limits: the function is not part of the Java configuration, so
`getFormatter()` does not return it, and every option that needs a function
needs its own small script.

## How to run

```
mvn -o -Pit clean verify -Dit.test=ChartsCspIT
```

The `it` profile builds the production bundle, starts the application on port
8080 and runs `ChartsCspIT`, a Playwright test with one method per check. It
uses the installed Chrome (Playwright channel `chrome`). After each test it
asserts that the page reported no CSP violation and no console error.

## Result

All 9 tests of `ChartsCspIT` pass (2026-11-18), with the production bundle and
`vaadin.csp=strict`:

- the strict header has no `unsafe-eval`
- the column chart renders its data labels from a format string (`12 kg`)
- the template helpers work: y axis labels such as `0k`, and data labels
  `low` and `high`
- hovering a column shows the tooltip header (`Apples`), point
  (`Weight: 12 kg`) and footer (`per crate`) formats
- the legend label format renders `Weight series`
- number formatting renders y axis labels such as `2.00`, and the helpers
  `ucfirst`, `multiply` and `#unless` render `Apples: 6 small` and `Pears: 16`
- a point added from the server renders (`value 20`)
- the chart with a string formatter is refused on the server with
  `CspViolationException`, before anything reaches the browser
- the declared function formats the data labels (`2k*`, `4k*`, `8k*`)

The browser reports no CSP violation and no console error in any test.

Note for tests: Highcharts draws an outline copy of a data label inside the
same SVG `text` element, separated by a zero-width space, so the text content
is for example `12 kg\u200B12 kg`.
