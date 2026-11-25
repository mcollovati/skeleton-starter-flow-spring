# Optimized bundle with add-on CSP fallbacks (vaadin/flow#27077, #27078)

This branch is `repro/addon-csp-fallback-layer1` plus two add-on classes
(`com.acme.addon.UnusedDefinition`, `com.acme.addon.ReflectedJs`), a view and
an IT for a definition that is really missing. It checks that the production build with
`optimizeBundle` (the default of `build-frontend`) still carries the
`@JsStringFallback` interfaces of the application. Nothing in the
application refers to them: Flow finds them by the string an add-on sends.
So the bytecode scanner never reaches them, and vaadin/flow#27077 must keep
them for another reason.

`UnusedDefinition` has a `@JsDefinition` and a `@DomEvent` with a filter and
event data. No route reaches it, so with #27077 none of its JavaScript may be
in the bundle.

## Steps

Install Flow with the change (flow-bom, flow-server, flow-build-tools,
vaadin-dev-server, flow-client, flow-maven-plugin), then:

```
rm -rf app/src/main/bundles
mvn install -DskipTests -pl addon
mvn -Pit verify -pl app
grep -c 'acme-unused\|AcmeUnusedKey' app/src/main/frontend/generated/vaadin-js-definitions.js
```

## Results (2026-11-25, strict, production mode)

| Flow | AddonCspIT | `UnusedDefinition` JavaScript in the bundle | Fallback functions in the bundle |
| --- | --- | --- | --- |
| main b75785d3d1 | 17 of 17 | yes (3: definition, filter, event data) | yes |
| #27078 d634b64873 merged with #27077 6602fca52d | 17 of 17 | no (0) | yes (calls, functions, filter and event data) |

With #27077, `js-string-fallbacks.json` lists both fallback interfaces, and
`jsDefinitionFunctionIds` in `stats.json` has all their ids. In the
`uses-fix` run below, the server log has no `ERROR` or `WARN` line, so #27078
does not report the fallbacks or the reached `EnterField` event as missing.

## A definition that is really missing (vaadin/flow#27078)

`com.acme.addon.ReflectedJs` is a `@JsDefinition` that
`MissingDefinitionView` (`/missing`) loads by its name with `Class.forName`
and calls through reflection. The scanner can not follow that, so with
#27077 the bundle does not carry it. The `uses-fix` profile adds
`UsesFixView` (`/uses-fix`) with `@Uses(ReflectedJs.class)`, the fix that the
error suggests.

`MissingDefinitionIT` presses the button twice and reads the server log,
which the `it` profile writes to `app/target/server.log`:

```
rm -rf app/src/main/bundles
mvn clean -Pit verify -pl app              # without the fix
rm -rf app/src/main/bundles
mvn clean -Pit,uses-fix verify -pl app     # with the fix
```

Delete `app/src/main/bundles` when you switch: a bundle saved by one run is
reused by the next. `clean` empties the server log, which is appended to.

| Flow | Profile | Result |
| --- | --- | --- |
| #27078 + #27077 | (none) | the call does nothing in the browser, and the browser console shows no error; the server logs one `ERROR` for two calls: `com.acme.addon.ReflectedJs declares JavaScript that was not included when creating the production bundle, so using it fails in the browser. ... If needed add a @Uses(ReflectedJs.class) to a class that the application uses.` |
| #27078 + #27077 | `uses-fix` | the call works; no `ERROR` or `WARN` in the server log |
| main b75785d3d1 | (none) | the call works (every definition is collected), so the IT fails on purpose: `Expected: Not marked`, `Received: Marked by ReflectedJs` |

`AddonCspIT` passes 17 of 17 in all three runs.

## Notes

- The fallback calls do not go through `JsDefinitionProxy`, so the runtime
  check of #27078 does not cover them. That is fine while #27077 always keeps
  the fallbacks.
- Merging #27077 into #27078 breaks a test of #27077 (`BundleValidationTest`
  uses `BundleValidationUtil.JS_DEFINITION_FUNCTION_IDS_STATS_KEY`, which
  #27078 moved to `BundleUtils`). Main code compiles.

---

# Add-on CSP fallback reproducer (vaadin/flow#26153)

An application runs with `vaadin.contentSecurityPolicy=strict` and uses a third-party add-on that
sends JavaScript as strings. The application developer cannot change the
add-on. This project shows what breaks, as a base for designing a way for the
application to provide a fallback.

Vaadin 25.4.0-beta1, Spring Boot.

## Modules

- `addon` (`com.acme.addon`): stands for an add-on from the Directory. Each
  class uses one feature that strict CSP refuses.
- `app` (`org.vaadin.example`): uses the add-on. `vaadin.contentSecurityPolicy=strict` is set in
  `app/src/main/resources/application.properties`.

## Cases

| # | Add-on class | Feature | Under strict |
|---|---|---|---|
| 1 | `AutoFocus` | `Element.executeJs(String)` in `onAttach` | refused in Java, the whole view fails |
| 2 | `ScrollPanel.scrollToBottom` | `Element.executeJs(String)`, constant | refused in Java |
| 3 | `ScrollPanel.getScrollTop` | `Element.executeJs(String)` with a result | refused in Java |
| 4 | `PageTitle` | `Page.executeJs(String, Object...)` (no `this`) | refused in Java |
| 5 | `Highlighter` | string built by concatenation | refused in Java |
| 6 | `ConsoleLogger` | text depends on the parameter count | refused in Java |
| 7 | `EscapeListener` | `setFilter(String)`, not a property path | refused in Java |
| 8 | `CharCounter` | deprecated `addEventData(String)`, not a path | refused in Java |
| 9 | `ResizeWatcher` | `addJsInitializer(String, Object...)` | refused in Java |
| 10 | `Tooltip` | `JsFunction.of(String, Object...)` | refused in Java |
| 11 | `LazyLoader` | `Page.addDynamicImport(String)` | refused in Java |
| 12 | `InlineHandler` | inline `onclick` attribute | refused by the browser only |
| 13 | `EvalWidget` | `new Function` in the add-on's own web component | refused by the browser only |
| 14 | `EnterField` | `@DomEvent` filter + `@EventData` expression | works (control) |
| 15 | `ClickReporter` | `JsFunction.of(String, Object...).withArguments("type")`, given to `executeJs(String, Object...)` | refused in Java |

Case 14 works because Flow finds `@DomEvent`/`@EventData` expressions in the
add-on at build time and puts them in the bundle. The string APIs have no
such step.

## How to run

Flow sends the CSP header only in production mode (the dev server needs
eval). In development mode the Java refusals (cases 1-11) happen, but the
browser does not block cases 12 and 13.

Production mode (shows all cases):

```
./mvnw install -DskipTests
java -jar app/target/app-1.0-SNAPSHOT.jar
```

Development mode (cases 1-11 only):

```
./mvnw install -DskipTests -pl addon
./mvnw -pl app spring-boot:run
```

Open http://localhost:8080/ and press "Run" on each case. The view catches
`ContentSecurityPolicyViolationException` only to show the result in place.
http://localhost:8080/plain uses `AutoFocus` the normal way and shows the
error page.

To see what `warn` reports, add `-Dvaadin.contentSecurityPolicy=warn`: every case works, and the
log has one warning per string call with the add-on class in the stack trace.
The warning does not include the JavaScript text.

## Results (checked 2026-11-19, production jar, Chromium)

- strict: cases 1-11 `REFUSED in Java: ContentSecurityPolicyViolationException`; case 12 the
  browser logs "Executing inline event handler violates ... script-src";
  case 13 `EvalError`; case 14 works; `/plain` shows the navigation error.
- warn: all 14 cases work, 11 warnings in the log.

## Browser tests

`app/src/test/java/org/vaadin/example/AddonCspIT.java` checks each case in
Chrome (Playwright for Java, using the installed Chrome; nothing is
downloaded). Every test asserts that its case **works**: no
`ContentSecurityPolicyViolationException` in Java, the effect is visible in the browser, and no
CSP violation is reported. So the report is the list of what works (passed)
and what does not (failed, with the reason). The build fails as long as one
case does not work.

```
./mvnw install -DskipTests -pl addon
./mvnw -Pit verify -pl app
```

The `it` profile starts the application in production mode, because Flow
sends the CSP header only there. Properties:

- `-Dit.port=8094`: port (default 8080)
- `-Dit.csp=warn`: CSP mode (default `strict`)

Results on 2026-11-19 (16 tests):

| Run | Passed | Failed |
|---|---|---|
| strict | header, 14 | 1, 1b, 2-11 (refused in Java), 12, 13 (blocked by the browser) |
| warn | all cases | header (warn sends no policy) |

The warn run shows that every case check can pass, so a failure under strict
is a real one.

## Fallback prototype (branch `repro/addon-csp-fallback-layer1`)

This branch runs against the Flow branch `csp-addon-fallback`. There, an
application marks a `@JsDefinition` interface with `@JsStringFallback`, and a
string that matches what it declares runs a function from the bundle instead
of being refused.

- `AcmeCspFallbacks` (extends `JsInvokers`): calls for the `executeJs`
  strings (cases 1-4, 6), each `@JsExpression` value the add-on's exact
  string with as many parameters as the add-on passes, and the filter
  (case 7) and event data (case 8) listed in the annotation. Case 15 has its
  call here too.
- `AcmeCspFunctionFallbacks` (extends `JsSnippets`): methods returning
  `JsFunction` for the functions made from strings: `addJsInitializer`
  (case 9), `JsFunction.of` (case 10) and `addDynamicImport` (case 11). They
  need their own interface, since one interface can not extend both
  `JsInvokers` and `JsSnippets`. The function of case 15 declares
  `@JsFunctionParameterNames("type")`, the names the add-on gives to
  `withArguments`.

Nothing registers them: the build lists them in
`META-INF/VAADIN/config/js-string-fallbacks.json`, which the server reads.

The root `pom.xml` imports `flow-bom` 25.4-SNAPSHOT above `vaadin-bom`, so
all Flow modules are snapshots and everything else is 25.4.0-beta1. Install
the changed modules and the BOM from the Flow branch first (the other Flow
modules come from the snapshot repository):

```
mvn install -DskipTests -pl flow-bom,flow-server,flow-build-tools,vaadin-dev-server
```

Results on 2026-11-19:

- Browser tests, strict, production mode: 17 of 17 pass (2026-11-20, with
  case 15). Again 17 of 17 on 2026-11-25, on the PR head 60ebba0e1c (Flow
  25.5-SNAPSHOT, after vaadin/flow#27076 and the `vaadin.contentSecurityPolicy`
  rename). Case 5 now sends 5 colors, because Flow adds the "not supported"
  note only after 5 different strings from one call site. A prod bundle built
  without a fallback is rebuilt when the fallback is added ("Detected
  JavaScript that the bundle does not carry, declared by: ...AcmeCspFallbacks"). Cases 5, 12 and 13 are not supported, so their tests assert the
  failure instead: case 5 is refused in Java (the string changes with the
  color) and the message says that `@JsStringFallback` does not support it;
  cases 12 and 13 are blocked by the browser.
- Development mode (`spring-boot:run`, strict): the dev server writes the
  list at startup, cases 1-4 and 6 run, `/plain` opens.
- Hotswap with the dev loop (`vaadin-dev apply --no-restart`, dev mode,
  strict): removing `@JsStringFallback` and applying deletes the list and
  cases 1-4 and 6 are refused again; adding it back and applying writes the
  list and they work again, without a restart. (The functions stay in the
  bundle, since the interface is a `@JsDefinition` either way.)
- Hotswap of a new filter (2026-11-20, dev loop, dev mode, strict): change
  `EscapeListener` to `setFilter("event.key === 'Delete'")`, as a new add-on
  release would, add that string to `filters` of `AcmeCspFallbacks`, and run
  `vaadin-dev apply --no-restart`. (Use a key nothing else declares:
  `EnterField` already has a function for `Enter`.)
  - With `-Dvaadin.frontend.hotdeploy=true` in `VAADIN_DEV_DAEMON_OPTS`
    (Vite running): the function is added to
    `src/main/frontend/generated/vaadin-js-definitions.js`, and case 7 fires
    on Delete with nothing compiled in the browser.
  - With the default dev bundle: Flow logs that the bundle does not carry the
    JavaScript of `AcmeCspFallbacks` until a restart. Case 7 still fires,
    because without a CSP header the browser compiles the filter and logs a
    console warning.
  - `apply --no-restart` reports that the change needs a restart ("frontend
    imports changed (AcmeCspFallbacks) ... escalating to restart"), as for a
    changed method of a `@JsDefinition`, so a plain `apply` restarts.

For the same tests without fallbacks, use the branch
`repro/addon-csp-fallback`.
