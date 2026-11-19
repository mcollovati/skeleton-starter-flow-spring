# Add-on CSP fallback reproducer (vaadin/flow#26153)

An application runs with `vaadin.csp=strict` and uses a third-party add-on that
sends JavaScript as strings. The application developer cannot change the
add-on. This project shows what breaks, as a base for designing a way for the
application to provide a fallback.

Vaadin 25.4.0-beta1, Spring Boot.

## Modules

- `addon` (`com.acme.addon`): stands for an add-on from the Directory. Each
  class uses one feature that strict CSP refuses.
- `app` (`org.vaadin.example`): uses the add-on. `vaadin.csp=strict` is set in
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
`CspViolationException` only to show the result in place.
http://localhost:8080/plain uses `AutoFocus` the normal way and shows the
error page.

To see what `warn` reports, add `-Dvaadin.csp=warn`: every case works, and the
log has one warning per string call with the add-on class in the stack trace.
The warning does not include the JavaScript text.

## Results (checked 2026-11-19, production jar, Chromium)

- strict: cases 1-11 `REFUSED in Java: CspViolationException`; case 12 the
  browser logs "Executing inline event handler violates ... script-src";
  case 13 `EvalError`; case 14 works; `/plain` shows the navigation error.
- warn: all 14 cases work, 11 warnings in the log.

## Browser tests

`app/src/test/java/org/vaadin/example/AddonCspIT.java` checks each case in
Chrome (Playwright for Java, using the installed Chrome; nothing is
downloaded). Every test asserts that its case **works**: no
`CspViolationException` in Java, the effect is visible in the browser, and no
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
string `executeJs` call whose text and parameter count match a method of it
runs that method's bundled function instead of being refused.

- `AcmeCspFallbacks`: the marked interface. Each `@JsExpression` value is
  the add-on's exact string, with as many parameters as the add-on passes.
  Nothing registers it: the build lists it in
  `META-INF/VAADIN/config/js-string-fallbacks.json`, which the server reads.

The root `pom.xml` imports `flow-bom` 25.4-SNAPSHOT above `vaadin-bom`, so
all Flow modules are snapshots and everything else is 25.4.0-beta1. Install
the changed modules and the BOM from the Flow branch first (the other Flow
modules come from the snapshot repository):

```
mvn install -DskipTests -pl flow-bom,flow-server,flow-build-tools,vaadin-dev-server
```

Results on 2026-11-19:

- Browser tests, strict, production mode: 8 of 16 pass (header, 1, 1b, 2, 3,
  4, 6, 14). Still failing: 5 (the string changes with the color), 7-11 (not
  `executeJs`), 12 and 13 (blocked by the browser).
- Development mode (`spring-boot:run`, strict): the dev server writes the
  list at startup, cases 1-4 and 6 run, `/plain` opens.
- Hotswap with the dev loop (`vaadin-dev apply --no-restart`, dev mode,
  strict): removing `@JsStringFallback` and applying deletes the list and
  cases 1-4 and 6 are refused again; adding it back and applying writes the
  list and they work again, without a restart. (The functions stay in the
  bundle, since the interface is a `@JsDefinition` either way.)

For the same tests without fallbacks, use the branch
`repro/addon-csp-fallback`.
