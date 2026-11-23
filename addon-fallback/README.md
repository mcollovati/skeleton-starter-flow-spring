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
string that matches what it declares runs a function from the bundle instead
of being refused.

- `AcmeCspFallbacks`: calls for the `executeJs` strings (cases 1-4, 6), each
  `@JsExpression` value the add-on's exact string with as many parameters as
  the add-on passes, and the filter (case 7) and event data (case 8) listed in
  the annotation. Case 15 has its call here too.
- `AcmeCspFunctionFallbacks`: methods returning `JsFunction` for the
  functions made from strings: `addJsInitializer` (case 9), `JsFunction.of`
  (case 10) and `addDynamicImport` (case 11). They need their own interface,
  since one interface can not mix calls and functions. The function of case
  15 declares `@JsFunctionParameterNames("type")`, the names the add-on gives
  to `withArguments`.

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
  case 15). Cases 5, 12 and 13 are not supported, so their tests assert the
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

## Real add-ons (branch `repro/addon-csp-fallback-ecosystem`)

This branch adds add-ons from the
[Vaadin ecosystem build](https://github.com/mstahv/vaadin-ecosystem-build/blob/main/EcosystemBuild.java)
that send JavaScript as strings, each with one view and one Playwright test
in `EcosystemCspIT`. The fallback interfaces are in
`app/src/main/java/org/vaadin/example/ecosystem/fallbacks`. Each declaration
was pasted from the message that Flow printed for the refused call; only some
method names were changed. The review of all 31 add-ons is in
https://github.com/vaadin/flow/issues/26153#issuecomment-7035443652.

Run only these tests (production mode, `vaadin.csp=strict`):

```
./mvnw install -DskipTests
./mvnw -Pit verify -pl app -Dit.test=EcosystemCspIT
```

With the application running, each view opens at
`http://localhost:8080/eco/<path>`.

Results on 2026-11-20 (Vaadin 25.4.0-beta1, Flow branch
`csp-addon-fallback`): 17 of 21 tests pass.

| Add-on | Version | View (source · URL) | What the test does | Fallbacks | Result |
|---|---|---|---|---|---|
| flow-viritin | 3.9.1 | [`ViritinView`](app/src/main/java/org/vaadin/example/ecosystem/ViritinView.java) · [/eco/viritin](http://localhost:8080/eco/viritin) | RichText, VScroller, WebStorage | [`FlowViritinCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/FlowViritinCspFallbacks.java) | pass |
| flow-viritin | 3.9.1 | [`ViritinMarkdownView`](app/src/main/java/org/vaadin/example/ecosystem/ViritinMarkdownView.java) · [/eco/viritin-markdown](http://localhost:8080/eco/viritin-markdown) | MarkdownMessage (element as `$0`) | [`FlowViritinCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/FlowViritinCspFallbacks.java) | pass |
| ckeditor-vaadin | 6.1.0 | [`CkEditorView`](app/src/main/java/org/vaadin/example/ecosystem/CkEditorView.java) · [/eco/ckeditor](http://localhost:8080/eco/ckeditor) | setValue, setReadOnly, insertText | [`VaadinCkEditorCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/VaadinCkEditorCspFallbacks.java) | pass |
| sortable-layout | 3.0.1 | [`SortableLayoutView`](app/src/main/java/org/vaadin/example/ecosystem/SortableLayoutView.java) · [/eco/sortable](http://localhost:8080/eco/sortable) | drag C above A | [`SortableLayoutCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/SortableLayoutCspFallbacks.java) | pass |
| hugerte-for-flow | 2.3.0 | [`HugeRteView`](app/src/main/java/org/vaadin/example/ecosystem/HugeRteView.java) · [/eco/hugerte](http://localhost:8080/eco/hugerte) | set the old value again after typing | [`HugeRteCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/HugeRteCspFallbacks.java) | pass |
| superfields | 0.21.2 | [`SuperFieldsView`](app/src/main/java/org/vaadin/example/ecosystem/SuperFieldsView.java) · [/eco/superfields](http://localhost:8080/eco/superfields) | read-only GridSelect, SuperTextField input mode | [`SuperFieldsCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/SuperFieldsCspFallbacks.java) | fail: add-on bug on Vaadin 25 (input mode), also with `vaadin.csp=off` |
| superfields | 0.21.2 | [`SuperFieldsMultiView`](app/src/main/java/org/vaadin/example/ecosystem/SuperFieldsMultiView.java) · [/eco/superfields-multi](http://localhost:8080/eco/superfields-multi) | GridMultiSelect (element as `$0`) | [`SuperFieldsCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/SuperFieldsCspFallbacks.java) | pass |
| enhanced-tabs-addon | 1.2.2 | [`EnhancedTabsView`](app/src/main/java/org/vaadin/example/ecosystem/EnhancedTabsView.java) · [/eco/enhanced-tabs](http://localhost:8080/eco/enhanced-tabs) | pick a tab in the overflow menu | [`EnhancedTabsCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/EnhancedTabsCspFallbacks.java), [`VaadinComponentsCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/VaadinComponentsCspFallbacks.java) | fail: add-on bug on Vaadin 25 (menu), also with `vaadin.csp=off` |
| twincolgrid | 4.0.1 | [`TwinColGridView`](app/src/main/java/org/vaadin/example/ecosystem/TwinColGridView.java) · [/eco/twincolgrid](http://localhost:8080/eco/twincolgrid) | no selection column, click a row and add it | [`TwinColGridCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/TwinColGridCspFallbacks.java) | pass |
| markdown-editor-addon | 2.0.3 | [`MarkdownViewerView`](app/src/main/java/org/vaadin/example/ecosystem/MarkdownViewerView.java) · [/eco/markdown](http://localhost:8080/eco/markdown) | MarkdownViewer shown and removed | [`MarkdownEditorCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/MarkdownEditorCspFallbacks.java) | pass |
| image-crop-addon | 1.3.0 | [`ImageCropView`](app/src/main/java/org/vaadin/example/ecosystem/ImageCropView.java) · [/eco/image-crop](http://localhost:8080/eco/image-crop) | setCrop draws the selection | [`ImageCropCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/ImageCropCspFallbacks.java) | pass |
| zoomist-addon | 1.3.0 | [`ZoomistView`](app/src/main/java/org/vaadin/example/ecosystem/ZoomistView.java) · [/eco/zoomist](http://localhost:8080/eco/zoomist) | getContainerData | [`ZoomistCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/ZoomistCspFallbacks.java) | pass |
| xterm-console | 3.3.0 | [`XTermView`](app/src/main/java/org/vaadin/example/ecosystem/XTermView.java) · [/eco/xterm](http://localhost:8080/eco/xterm) | writeln | [`XTermConsoleCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/XTermConsoleCspFallbacks.java) | pass |
| year-month-calendar | 4.6.2 | [`YearMonthCalendarView`](app/src/main/java/org/vaadin/example/ecosystem/YearMonthCalendarView.java) · [/eco/year-month-calendar](http://localhost:8080/eco/year-month-calendar) | class name generator | [`YearMonthCalendarCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/YearMonthCalendarCspFallbacks.java) | pass |
| chat-assistant-addon | 5.2.0 | [`ChatAssistantView`](app/src/main/java/org/vaadin/example/ecosystem/ChatAssistantView.java) · [/eco/chat-assistant](http://localhost:8080/eco/chat-assistant) | open the chat window with a message | [`ChatAssistantCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/ChatAssistantCspFallbacks.java), [`VaadinComponentsCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/VaadinComponentsCspFallbacks.java) | fail: all strings match, but `connect-src` has no `data:`, so `vaadin-icon` can not load a `data:` icon |
| extended-login | 1.1.0 | [`ExtendedLoginView`](app/src/main/java/org/vaadin/example/ecosystem/ExtendedLoginView.java) · [/eco/extended-login](http://localhost:8080/eco/extended-login) | replaced header, no forgot password link | [`ExtendedLoginCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/ExtendedLoginCspFallbacks.java) | pass |
| chipfield-addon | 2.8.0 | [`ChipFieldView`](app/src/main/java/org/vaadin/example/ecosystem/ChipFieldView.java) · [/eco/chipfield](http://localhost:8080/eco/chipfield) | chips for the value | [`ChipFieldCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/ChipFieldCspFallbacks.java) | pass |
| orgchart-addon | 5.4.0 | [`OrgChartView`](app/src/main/java/org/vaadin/example/ecosystem/OrgChartView.java) · [/eco/orgchart](http://localhost:8080/eco/orgchart) | chart shown, add a child | [`OrgChartCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/OrgChartCspFallbacks.java) | pass |
| fullcalendar2 | 8.0.0 | [`FullCalendarView`](app/src/main/java/org/vaadin/example/ecosystem/FullCalendarView.java) · [/eco/fullcalendar](http://localhost:8080/eco/fullcalendar) | default setup | [`FullCalendarCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/FullCalendarCspFallbacks.java) | fail, expected: the add-on's own client uses `new Function` (browser only) |
| fullcalendar2 | 8.0.0 | [`FullCalendarSelectView`](app/src/main/java/org/vaadin/example/ecosystem/FullCalendarSelectView.java) · [/eco/fullcalendar-select](http://localhost:8080/eco/fullcalendar-select) | select days and clear the selection | [`FullCalendarCspFallbacks`](app/src/main/java/org/vaadin/example/ecosystem/fallbacks/FullCalendarCspFallbacks.java) | pass |
| maplibre | 2.1.1 | [`MapLibreView`](app/src/main/java/org/vaadin/example/ecosystem/MapLibreView.java) · [/eco/maplibre](http://localhost:8080/eco/maplibre) | the test asserts the refusal | none: the strings contain ids and coordinates | pass (refused, not supported) |

`VaadinComponentsCspFallbacks` covers the string JavaScript of Vaadin's own
`MenuBar` and `Dialog` in 25.4.0-beta1. It is already fixed in
flow-components, so it will not be needed after the next release.
