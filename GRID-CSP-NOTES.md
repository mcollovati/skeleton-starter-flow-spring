# Grid and TreeGrid under `vaadin.csp=strict`

The `/grid-csp` view (`GridCspView`) runs Grid and TreeGrid with a strict
Content Security Policy: `script-src 'nonce-…' 'strict-dynamic'`, without
`unsafe-eval`.

## What it needs

- Flow PR vaadin/flow#26960 (`JsSnippet` sub-interfaces as snippet types),
  installed as 25.4-SNAPSHOT: `flow-server`, `flow-build-tools`,
  `flow-maven-plugin`.
- flow-components branch `prototype/grid-lit-template-csp` (declared Lit
  templates, `LitRenderer.of(CompiledLitTemplate)`), installed as 25.4-SNAPSHOT:
  `vaadin-renderer-flow`, `vaadin-grid-flow`.

Build offline (`-o`), so Maven does not replace the local snapshots with
remote ones.

## What the view covers

- Grid: path column, component column, `TextRenderer`, `NumberRenderer`,
  `LocalDateRenderer`, `NativeButtonRenderer` (click reaches the server), item
  details with a `ComponentRenderer`.
- An application template, `PersonTemplates.statusBadge(String)`, declared as
  a `CompiledLitTemplate` with a capture (`$0`), a property and a function
  (`functions.select`).
- TreeGrid: `addHierarchyColumn` and `addComponentHierarchyColumn`, both
  expanding.
- A features grid: header component, hidden header component, footer text and
  component, a hidden column, a tooltip generator, the editor, sorting from the
  header and from the server, `setPageSize`, `scrollToEnd`. Together with the
  grids above, this runs every method of `GridConnectorJs`, `GridJs`,
  `EditorJs`, `TreeGridConnectorJs`, `LitRendererJs` and the declared templates
  in the browser.
- Replacing the item details renderer, which unsets the old renderer on the
  client. The check opens another row to see the new renderer: details that
  are already open stay empty, because `DetailsManager.setRenderer` in Grid does
  not refresh the data of open items for the new renderer. This is an existing
  bug, reproduced without the CSP changes.
- A button that adds a grid with a string template. Strict mode refuses it on
  the server with `CspViolationException`.

## How to run

```
mvn -o -Pit clean verify
```

The `it` profile builds the production bundle, starts the application on port
8080 and runs `GridCspIT`, a Playwright test with one method per check. It uses
the installed Chrome (Playwright channel `chrome`), so no browser is
downloaded. After each test it asserts that the page reported no CSP violation
and no console error.

To run the test against an application that is already running, for example
from `java -jar`:

```
mvn -o test-compile failsafe:integration-test failsafe:verify -Dit.test=GridCspIT
```

Set `-Dit.baseUrl=...` for another address than `http://localhost:8080`.

## Result

All 17 tests pass (2026-11-11): the strict header, every column renders, the
functions reach the server, both hierarchy columns expand, every Grid feature
above works, the string template is refused, and the browser reports no CSP
violation and no console error.

Not covered: `GridContextMenu` (context-menu module not migrated yet) and Grid
Pro.
