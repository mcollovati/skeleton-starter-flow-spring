# `@EventData` from the bundle under `vaadin.csp=strict`

Cases for vaadin/flow#27000 (fixes vaadin/flow#26881). An `@EventData` value
that is not a property path is generated into the frontend bundle at build
time, and the browser runs it without compiling anything. The
`/bundled-event-data` view (`BundledEventDataView`) runs with
`vaadin.csp=strict`.

## What it needs

Flow from the PR branch `feat/bundle-event-data-expressions`, installed as
25.4-SNAPSHOT: `flow-server`, `flow-client`, `flow-build-tools`,
`flow-maven-plugin`, `vaadin-spring`, `vaadin-dev-server`, `flow-dnd`. The
other 25.4-SNAPSHOT artifacts (components, BOM) must already be in the local
repository. Build offline (`-o`), so Maven does not replace the local
snapshots with remote ones.

## Cases

Setup cases run when the view is created. Each one is a row in the "Setup
results" list, with the outcome (`OK` or the exception name) in
`data-outcome`. Click cases write what the server got next to the area.

| Case | What it checks | Expected |
|------|----------------|----------|
| A1 | one `@DomEvent` with two expressions (`event.detail * 10`, `event.type.toUpperCase()`), a path and an `Element` path | all four values |
| A2 | the same expression in two event classes (`click`, `dblclick`) | both work; the production bundle has one function for it |
| A3 | a template literal, a value ending with a `//` comment, a value on two lines, single and double quotes and a backslash | the values survive the code generation |
| A4 | `DropEvent` of `flow-dnd` (an event class from a jar), whose offsets are expressions | offsets received |
| A1-A4 | every bundled value is a function in `window.Vaadin.Flow.jsSnippets` | registered |
| A5 | `@EventData("event.target.closest('div')") Element`: mapped to an element, so not bundled | `CspViolationException` when the listener is added |
| A6 | the A1 expression through `addEventData(String)` | `CspViolationException`: only `@EventData` uses the bundle |

## How to run

```
mvn -o -Pit clean verify
```

The `it` profile builds the production bundle, starts the application on port
8080 and runs `BundledEventDataIT`, a Playwright test with one method per case.
It uses the installed Chrome (Playwright channel `chrome`), so no browser is
downloaded. After each test it checks that the page reported no CSP violation
and no console error. `sharedExpression_oneFunctionInBundle` reads the bundle
from `target/classes/META-INF/VAADIN/webapp/VAADIN/build`, so it needs the
production build.

To run the test against an application that is already running:

```
mvn -o test-compile failsafe:integration-test failsafe:verify -Dit.test=BundledEventDataIT
```

Set `-Dit.baseUrl=...` for another address than `http://localhost:8080`.

## Result

Production mode (2026-11-12, PR head 223bc7657a): all 9 tests pass.

Development mode (`mvn -o clean spring-boot:run`, development bundle): the 8
browser tests ran against it. 7 pass. `strictCspHeader_noUnsafeEval` fails
because Flow sends the policy header only in production mode
(`IndexHtmlRequestHandler`), which is by design. So in development mode the
tests check that the development bundle, with the readable names of the
functions, loads and works; they do not check CSP. The names in
`src/main/frontend/generated/vaadin-js-definitions.js` escape the quotes, the
backslash and the line break of A3 correctly.

## Findings

- The generated file also has functions for event classes of flow-components,
  not only flow-dnd: `MapClickEvent` and `MapClusterClickEvent` (Map) and
  `SplitLayout.SplitterDragEndEvent`. The PR description names only flow-dnd as
  changing the bundle content.
- A5 is still refused under strict mode: an expression that maps to an element
  or component has no CSP-safe form yet, also not through
  `addEventDataElement(String)`.

Not covered: hotswap of a changed `@EventData` value (the PR logs an error and
sends `null` until a restart), and a missing function in the browser.
