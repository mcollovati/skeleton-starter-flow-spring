# `addEventDataPath` under `vaadin.csp=strict`

Cases for vaadin/flow#26999 (fixes vaadin/flow#26998). The `/event-data-path`
view (`EventDataPathView`) runs with `vaadin.csp=strict`, so the browser must
not compile any event data, and the server refuses event data that the browser
would compile.

The `/event-data-element` view (`EventDataElementView`) has the cases for
vaadin/flow#27001, see [below](#domeventdataelement-vaadinflow27001).

## What it needs

Flow with both PR branches merged, `feature/add-event-data-path` (#26999) and
`feature/event-data-element` (#27001), installed as 25.4-SNAPSHOT:
`flow-server`, `flow-client`, `flow-build-tools`, `flow-maven-plugin`,
`vaadin-spring`. The two PRs are not stacked, but they merge without
conflicts:

```
git fetch origin pull/26999/head:pr-26999 pull/27001/head:pr-27001
git switch --detach pr-26999
git merge pr-27001
mvn install -DskipTests -pl flow-server,flow-client,flow-build-tools,flow-plugins/flow-maven-plugin,vaadin-spring -am
```

The other 25.4-SNAPSHOT artifacts (components, BOM) must already be in the
local repository. Build offline (`-o`), so Maven does not replace the local
snapshots with remote ones.

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
| B2 | `addEventData(Class)` with a top-level `type` property next to `event.*` paths | `CspViolationException` (`type` is compiled), nothing added |
| B3 | `addEventDetail(Class)` with a `größe` property next to `width` | `CspViolationException` (`event.detail.größe` is compiled), nothing added |
| D1/D2 | deprecated `addEventData(String)` / `addEventDataElement(String)` with paths | still work |
| D3/D4 | deprecated methods with expressions | `CspViolationException` (strict mode, as before the PR) |
| E1 | `@DomEvent` with path `@EventData` values (now added through `ElementListenerMap.addEventData`) | works |
| E2 | `@DomEvent` with `@EventData("event.detail + 1")` | `CspViolationException` when the listener is added |
| T1 | `preventDefault()` on a link | listener runs, the link is not followed |
| T2 | `stopPropagation()` on an inner button | the outer listener is not called, but is for a click on the outer element |
| T3 | `synchronizeProperty()` through `addPropertyChangeListener("value", "change", ...)` | value synchronized |
| W1 | `Page.addBrowserWindowResizeListener` (now uses `addEventDataPath`) | new window size received |

B2 and B3 changed with the PR: its first version refused them with
`IllegalArgumentException`. Since `da29a7ba02` and `1992c6915a` the bean
methods accept any property name again, and a name that is not a path is
compiled, like before the PR. So strict mode refuses them.

## `DomEventData<Element>` (vaadin/flow#27001)

Event data declared as `DomEventData<Element>` gives the server-side element of
the DOM node that the expression returns. It is the CSP-safe replacement for
`addEventDataElement(String)` with an expression, which D4 shows refused.

| Case | What it checks | Expected |
|------|----------------|----------|
| X1 | `event.composedPath()[0]` on a click inside a shadow root, next to `mapEventTargetElement()`, and the `FocusTracker` expression of vaadin/swingbridge-emulators | the inner element; `event.target` is the host; the document-level target is the host |
| X2 | `event.target.closest('div.row')`, the D4 expression, with a `DomEventData<String>` in the same listener | the row, also from `getEventData(DomEventData<Element>)`; the string stays a string |
| X3 | `element.querySelector($0)` with a captured selector | the element |
| X4 | `document.getElementById($0)` for an element outside the listener's element | found under the UI |
| X5 | `return null` and `return 42` | no element; `getEventData` gives `null` |
| X6 | an element that is hidden with `setVisible(false)` after it was shown, so the browser still has it | no element: the server does not report invisible elements |
| X7 | a `<b>` that the browser made from `innerHTML`, so it has no state node | the closest parent with a state node |

## How to run

```
mvn -o -Pit clean verify
```

The `it` profile builds the production bundle, starts the application on port
8080 and runs `EventDataPathIT` and `EventDataElementIT`, Playwright tests with
one method per case. They use the installed Chrome (Playwright channel
`chrome`), so no browser is downloaded. After each test they check that the
page reported no CSP violation and no console error.

To run the tests against an application that is already running:

```
mvn -o test-compile failsafe:integration-test failsafe:verify -Dit.test='EventDataPathIT,EventDataElementIT'
```

Set `-Dit.baseUrl=...` for another address than `http://localhost:8080`.

## Result

All 21 tests pass (2026-11-12, local merge of #26999 at c5199e0ae7 and #27001
at b467598653): 16 in `EventDataPathIT` and 5 in `EventDataElementIT`. Every
case in both tables gives the expected result, and the browser reports no CSP
violation and no console error. X5 sends `42` to the client lookup, which
fails quietly (a debug message only) and gives no element.

Not covered: a custom `DomListenerRegistration` implementation (the fallback to
`addEventData(String)` for tokens), and `addEventDetail(Class)` with a valid
bean, which needs an event with an object `detail` (the PR's unit tests cover
both).
