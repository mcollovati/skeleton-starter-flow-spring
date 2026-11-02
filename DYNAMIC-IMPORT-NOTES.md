# `Page.addDynamicImport(JsFunction)` use case (vaadin/flow#26813)

The `/dynamic-import` view (`DynamicImportView`) loads a large web component
only on first use:

- `ImportJs.loadEditor()` is a declared snippet with a literal
  `import('Frontend/report-editor/report-editor.js')`. The snippet is in the
  generated `vaadin-js-definitions.js`, so Vite sees the `import()` and puts
  the module in its own chunk (`VAADIN/build/report-editor-<hash>.js`).
- "Open advanced editor" calls `addDynamicImport(IMPORT_JS.loadEditor())` and
  adds `<report-editor>` in the same round trip. Flow applies no change until
  the import resolves, so the element is defined before Flow creates it.
- `ReportEditor` has no `@JsModule`: that would put the module in the chunk of
  the view, loaded when the view opens, not on first use.
- "Capture an attached/detached element" shows that a function that captures
  an element is rejected with `IllegalArgumentException`, attached or not.

## Verified (production build, headless Chromium, `vaadin.csp=warn`)

- Before the click: no request for the `report-editor` chunk; the module is not
  evaluated and the element is not defined.
- After the click: one request for `report-editor-<hash>.js`; the editor reads
  "defined before Flow created it: yes". A second click adds a second editor
  and makes no second request.
- Both capture buttons show the `IllegalArgumentException`.
- No CSP warning comes from `addDynamicImport` or from the route chunk loading.
  The warnings in the log come from Vaadin's own event data strings (`Button`
  click listeners, the `Page` resize listener), as described in
  `CSP-NOTES.md`.

## How to reproduce

```
# install flow-server, flow-build-tools, the plugins and flow-client from the
# PR branch feat/load-chunks-without-string-js first
mvn -Pproduction package -DskipTests
java -jar target/spring-skeleton-1.0-SNAPSHOT.jar
# open http://localhost:8080/dynamic-import and watch the network tab
```
