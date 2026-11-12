# `addEventDataPath` under `vaadin.csp=strict`

Cases for vaadin/flow#26999 (fixes vaadin/flow#26998). The `/event-data-path`
view (`EventDataPathView`) runs with `vaadin.csp=strict`, so the browser must
not compile any event data, and the server refuses event data that the browser
would compile.

## What it needs

Flow from the PR branch `feature/add-event-data-path`, installed as
25.4-SNAPSHOT: `flow-server`, `flow-client`, `flow-build-tools`,
`flow-maven-plugin`, `vaadin-spring`. The other 25.4-SNAPSHOT artifacts
(components, BOM) must already be in the local repository. Build offline
(`-o`), so Maven does not replace the local snapshots with remote ones.

## Cases

Setup cases run when the view is created. Each one is a row in the "Setup
results" list, with the outcome (`OK` or the exception name) in
`data-outcome`. Click cases write what the server got next to the button.

| Case | What it checks | Expected |
|------|----------------|----------|
| P1 | `addEventDataPath` with `event.type`, `event.detail`, `event.target.id`, `element.dataset.case`, `event.relatedTarget?.id` | values with the path as key; the `?.` path gives `undefined`, so its key is not in the JSON |
| P2 | `addEventDataElementPath` with `event.target.parentElement`, `event.target`, `element.parentElement` | the server-side elements; `event.target` is mapped like `mapEventTargetElement()` |
| R1 | both new methods with 12 strings that are not paths (expressions, `[0]`, `['x']`, empty, leading space, no `event`/`element` root, `..`, trailing `.`, a call, non-ASCII name) | `IllegalArgumentException` |
| R1 | both new methods with paths at the edge of the grammar (`event`, `element`, `event?.detail`, `element.$x_1`, `event.detail?.a?.b`) | `OK` |
| R1 | both new methods with `null` | `NullPointerException` |
| R2 | a refused string on a registration that already has `element.id` | refused, and a click sends only `element.id` |
| B1 | `addEventData(Class)` with a valid bean, decoded with `getEventData(Class)` | `type=click`, `detail=1` |
| B2 | `addEventData(Class)` with a top-level `type` property next to valid `event.*` ones | `IllegalArgumentException`, nothing added |
| B3 | `addEventDetail(Class)` with a `größe` property next to a valid `width` | `IllegalArgumentException`, nothing added |
| D1/D2 | deprecated `addEventData(String)` / `addEventDataElement(String)` with paths | still work |
| D3/D4 | deprecated methods with expressions | `CspViolationException` (strict mode, as before the PR) |
| E1 | `@DomEvent` with path `@EventData` values (now added through `ElementListenerMap.addEventData`) | works |
| E2 | `@DomEvent` with `@EventData("event.detail + 1")` | `CspViolationException` when the listener is added |
| T1 | `preventDefault()` on a link | listener runs, the link is not followed |
| T2 | `stopPropagation()` on an inner button | the outer listener is not called, but is for a click on the outer element |
| T3 | `synchronizeProperty()` through `addPropertyChangeListener("value", "change", ...)` | value synchronized |
| W1 | `Page.addBrowserWindowResizeListener` (now uses `addEventDataPath`) | new window size received |

B2 and B3 are the behaviour change in the PR: before it, B2 added the paths and
failed in the browser, and B3 worked as a compiled expression.

## How to run

```
mvn -o -Pit clean verify
```

The `it` profile builds the production bundle, starts the application on port
8080 and runs `EventDataPathIT`, a Playwright test with one method per case. It
uses the installed Chrome (Playwright channel `chrome`), so no browser is
downloaded. After each test it checks that the page reported no CSP violation
and no console error.

To run the test against an application that is already running:

```
mvn -o test-compile failsafe:integration-test failsafe:verify -Dit.test=EventDataPathIT
```

Set `-Dit.baseUrl=...` for another address than `http://localhost:8080`.

## Result

All 16 tests pass (2026-11-12, PR head d1d37f628c): every case in the table
gives the expected result, and the browser reports no CSP violation and no
console error.

Not covered: a custom `DomListenerRegistration` implementation (the fallback to
`addEventData(String)` for tokens), and `addEventDetail(Class)` with a valid
bean, which needs an event with an object `detail` (the PR's unit tests cover
both).
