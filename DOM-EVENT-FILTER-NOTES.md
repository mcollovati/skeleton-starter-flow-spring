# `@DomEvent` filters from the bundle under `vaadin.csp=strict`

Cases for vaadin/flow#27023 (part of vaadin/flow#26961). The
`/dom-event-filters` view (`DomEventFilterView`) runs in production mode with
`vaadin.csp=strict`. A `@DomEvent` filter that is not a property path used to
be compiled in the browser, so `strict` refused the listener. With the PR, the
build puts such filters into the bundle, and the browser runs them without
compiling anything.

## What it needs

Flow from the PR branch `feat/bundle-dom-event-filters` installed as
25.4-SNAPSHOT: `flow-server`, `flow-client`, `flow-build-tools`,
`flow-maven-plugin`, `vaadin-spring`, `vaadin-dev-server`:

```
git fetch origin pull/27023/head:pr-27023
git switch --detach pr-27023
mvn install -DskipTests -pl flow-server,flow-client,flow-build-tools,flow-plugins/flow-maven-plugin,vaadin-spring,vaadin-dev-server -am
```

The other 25.4-SNAPSHOT artifacts (components, BOM) must already be in the
local repository. Build offline (`-o`), so Maven does not replace the local
snapshots with remote ones.

Run the tests with `mvn -o -Pit verify`. They use the installed Chrome through
Playwright, so no browser is downloaded. The application starts on port 8080;
to use another port, add
`-Dspring-boot.run.jvmArguments=-Dserver.port=<port> -Dit.baseUrl=http://localhost:<port>`.

## Cases

Every test also checks that the page reported no Content Security Policy
violation and no console error or warning. A filter that the bundle does not
have would fall back to compiling, which the policy blocks and the client
reports, so a passing test also shows that the filter ran from the bundle.

| Case | What it checks | Expected |
|------|----------------|----------|
| Policy | the response of the view | `Content-Security-Policy` with a `script-src` nonce and no `unsafe-eval` |
| F1 | `keydown` with `filter = "event.key === 'Enter' && !event.repeat"`; `a`, then Enter held down (three keydowns), then `b` and Enter | one event for the held Enter, two in total |
| F2 | `click` with `filter = "element.dataset.enabled === 'true'"`; click while `false`, then the server sets `true` and the button is clicked again | only the second click is sent |
| F3 | `click` with `filter = "event.detail > 0 && !event.shiftKey"` and `@EventData` paths `event.detail`, `event.type`; Shift-click, then click | one event, with `detail=1 type=click` |
| F4 | `click` with `filter = "element.dataset.locked === 'yes'"` and `preventDefault = true` on a checkbox; click while locked, then unlock and click | locked: event sent and the checkbox stays unchecked; unlocked: no event and the checkbox is checked |
| F5 | `input` with `filter = "element.value.length >= 3"`, trailing debounce 300 ms, `@EventData("element.value")`; type `ab`, wait, type `cd` | nothing for `ab`; one event with `abcd` |
| F6 | an event type in another package (`org.vaadin.addon.events.AddonClickEvent`, `filter = "event.altKey === true"`); click, then Alt-click | one event |
| F7 | the same expression `event.button === 0` as the filter of one event type and as `@EventData` of another, both on one button | the filtered listener gets the click, the other gets `primary=true` |
| F8 | Flow's `KeyUpEvent`, whose filter is the path `event.key?.length` since vaadin/flow#27021 | the key-up is sent with its key |
| F9 | `setFilter("event.button === 0")` on a listener at runtime | `CspViolationException`: the PR does not change string filters set through the API |

## Not covered

- The fallback for a filter that the bundle does not have (the browser compiles
  it, warns once, and with a blocking policy logs an error and does not send
  the event). In a production build every `@DomEvent` type on the class path is
  in the bundle, so this case needs a filter that changed after the bundle was
  built, which is a development-mode scenario.
- The dev loop fingerprint for bundled filters.

## Results

Run on 2026-11-16 against the PR head cf05e7f365: all 10 tests pass.

Control against `main` at 991b9501eb (which has vaadin/flow#27021 but not
this PR): all 10 tests fail, because creating the view already throws
`CspViolationException` for the first `@DomEvent` filter that is not a path.

The build writes the compiled bundle to `src/main/bundles/`. Delete that folder
when you switch between Flow versions.
