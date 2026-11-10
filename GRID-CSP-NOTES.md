# Grid and TreeGrid under `vaadin.csp=strict`

The `/grid-csp` view (`GridCspView`) runs Grid and TreeGrid with a strict
Content Security Policy: `script-src 'nonce-…' 'strict-dynamic'`, without
`unsafe-eval`.

## What it needs

- Flow PR vaadin/flow#26960 (`JsSnippet` sub-interfaces as snippet types),
  installed as 25.4-SNAPSHOT: `flow-server`, `flow-build-tools`,
  `flow-maven-plugin`.
- flow-components branch `prototype/grid-lit-template-csp` (declared Lit
  templates, `LitRenderer.of(JsLitItemTemplate)`), installed as 25.4-SNAPSHOT:
  `vaadin-renderer-flow`, `vaadin-grid-flow`.

Build offline (`-o`), so Maven does not replace the local snapshots with
remote ones.

## What the view covers

- Grid: path column, component column, `TextRenderer`, `NumberRenderer`,
  `LocalDateRenderer`, `NativeButtonRenderer` (click reaches the server), item
  details with a `ComponentRenderer`.
- An application template, `PersonTemplates.statusBadge(String)`, declared as
  a `JsLitItemTemplate` with a capture (`$0`), a property and a function
  (`functions.select`).
- TreeGrid: `addHierarchyColumn` and `addComponentHierarchyColumn`, both
  expanding.
- A button that adds a grid with a string template. Strict mode refuses it on
  the server with `CspViolationException`.

## How to run

```
mvn clean package -DskipTests -o
java -jar target/spring-skeleton-1.0-SNAPSHOT.jar --vaadin.launch-browser=false
# in another shell; PLAYWRIGHT_CORE points to an installed playwright-core
PLAYWRIGHT_CORE=/path/to/node_modules/playwright-core node scripts/grid-csp-check.cjs
```

The script uses `/usr/bin/google-chrome` and saves `target/grid-csp.png`.

## Result (2026-11-10)

All 18 checks pass: the strict header, every column renders, the functions
reach the server, both hierarchy columns expand, the string template is
refused, and the browser reports no CSP violation and no console error.
