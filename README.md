# Optimized bundle with add-on CSP fallbacks (vaadin/flow#27077, #27078, #27441, #27423)

This branch is `repro/addon-csp-fallback-layer1` plus two add-on classes
(`com.acme.addon.UnusedDefinition`, `com.acme.addon.ReflectedJs`), a view and
an IT for a definition that is really missing, and a view and an IT for a
direct call to a string fallback interface. It checks that the production
build with `optimizeBundle` (the default of `build-frontend`) still carries
the `@JsStringFallback` interfaces of the application. Nothing in the
application refers to them: Flow finds them by the string an add-on sends.
So the bytecode scanner never reaches them, and vaadin/flow#27077 must keep
them for another reason.

`UnusedDefinition` has a `@JsDefinition` and a `@DomEvent` with a filter and
event data. No route reaches it, so with #27077 none of its JavaScript may be
in the bundle.

Every view has a menu on the left (`MainLayout`, applied with `@Layout`)
that links to all use cases, grouped by issue or PR.

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

## A direct call to a string fallback interface (vaadin/flow#27078)

`DirectFallbackView` (`/direct-fallback`) calls `AcmeCspFallbacks` directly,
as application code may do once the interface exists:
`panel.getElement().executeJs(AcmeCspFallbacks.class)`, then
`scrollToBottom()` and `scrollTop()`. The build registers these functions
under the ids of string fallbacks. So the bundle check must look for those
ids, not for the ids of the plain JavaScript. #27078 fixed this in
3f8c95b4fd. `DirectFallbackIT` checks that the call works and that the
server log has no line about `AcmeCspFallbacks`. It runs in the same
`mvn clean -Pit verify -pl app`.

| Flow | Result |
| --- | --- |
| #27078 2dbc299775 (before the fix) | the call works, but the server logs a wrong error: `org.vaadin.example.AcmeCspFallbacks declares JavaScript that was not included when creating the production bundle ...`, so the IT fails |
| #27078 e68a80763a (with the fix) | the call works and nothing is logged; 19 of 19 tests pass, with and without `uses-fix` |

## Retest on the #27078 head (2026-11-26)

#27078 e68a80763a contains #27077 6602fca52d. The results above are the
same there: 19 of 19 tests pass in both variants, `UnusedDefinition` is not
in the bundle, both fallback interfaces are listed and bundled, and the only
server log error is the expected one for `ReflectedJs` (without `uses-fix`).

## Methods without `@JsExpression` (vaadin/flow#27441)

With #27441, a method of an interface that extends `JsInvokers` can leave
out `@JsExpression`: `void focus();` runs `return this.focus();` on the
element. These cases check the shorthand in an add-on, in the production
bundle that `optimizeBundle` makes, under strict CSP.

Add-on classes:

- `ShorthandBox.BoxJs`: shorthand methods (no arguments, two arguments,
  variadic, `PendingJavaScriptResult`, `JavaScriptResult<Boolean>`, two
  overloads) and one annotated method. `ShorthandView` reaches it.
- `UnusedDefinition.UnusedShorthandJs`: a shorthand method that no route
  reaches, so it may not be in the bundle.
- `ReflectedShorthandJs`: like `ReflectedJs`, called only through
  reflection from `MissingShorthandView` (`/missing-shorthand`). The
  `uses-fix` profile adds `@Uses(ReflectedShorthandJs.class)`.

`ShorthandView` (`/shorthand`), package `org.vaadin.example.shorthand`:

| Row | Case | Expected |
| --- | --- | --- |
| S1 | `focus()` | the box has focus |
| S2 | `setAttribute(name, value)` | the attribute is set |
| S3 | `append(String...)` | the texts are appended (last argument spread) |
| S4 | `getAttribute(name)` returning `PendingJavaScriptResult` | the value comes back |
| S5 | `hasAttribute(name)` returning `JavaScriptResult<Boolean>` | `true` comes back |
| S6 | `toggleAttribute(name)` and `toggleAttribute(name, force)` | both attributes set |
| S7 | annotated `mark(value)` in the same interface | `data-acme` set |
| S8 | `SubBoxJs` (application) extends `BoxJs`, declares nothing | inherited methods work |
| S9 | `TypoJs.focuss()`, a typo | fails in the browser, the error handler gets it |
| R1 | `Page.executeJs(BoxJs.class)` | refused in Java: a page call runs on no element |
| R2 | `MixedInJs`: `blur()` mixed in from an interface that does not extend `JsInvokers` | refused in Java |
| R3 | `JsSnippets` interface with a method without `@JsExpression` | refused in Java |

`ShorthandIT` checks each row, that the bundle has `BoxJs` and not
`UnusedShorthandJs`, and the `/missing-shorthand` case (one `ERROR` that
names `ReflectedShorthandJs` without `uses-fix`, the call works with it). It
runs in the same commands as `MissingDefinitionIT`:

```
rm -rf app/src/main/bundles
mvn install -DskipTests -pl addon
mvn clean -Pit verify -pl app
rm -rf app/src/main/bundles
mvn clean -Pit,uses-fix verify -pl app
```

### Results (2026-11-26, #27441 d7b6049adb, strict, production mode)

33 of 33 tests pass, with and without `uses-fix` (`AddonCspIT` 17,
`ShorthandIT` 14, `MissingDefinitionIT` 1, `DirectFallbackIT` 1).

- S1-S8 work, with no browser console error. The bundle has the `BoxJs`
  functions (`this.append(...$0)`, `this.toggleAttribute($0, $1)`) and not
  `acmeUnusedShorthand`.
- S9: the error handler gets `TypeError: this.focuss is not a function`. The
  server does not notice the typo.
- R1: `IllegalArgumentException: com.acme.addon.ShorthandBox$BoxJs has append,
  focus, getAttribute, hasAttribute, setAttribute, toggleAttribute,
  toggleAttribute without @JsExpression, ...`. The overloaded name is listed
  twice.
- R2: `... MixedInJs declares no JavaScript to run for blur. Annotate every
  method with @JsExpression, or declare it in an interface that extends
  JsInvokers to call the method of the same name on the element`.
- R3: `... ShorthandSnippetsJs declares no JavaScript to run for focus.
  Annotate every method with @JsExpression`.
- The build logs `Leaving the JavaScript of ... out of the bundle` for
  `MixedInJs` and `ShorthandSnippetsJs` (both on purpose) and goes on.
- Without `uses-fix`: `/missing-shorthand` does nothing, no browser console
  error, and the server logs one `ERROR` for two calls:
  `com.acme.addon.ReflectedShorthandJs declares JavaScript that was not
  included when creating the production bundle ... add a
  @Uses(ReflectedShorthandJs.class) ...`. With `uses-fix` the call works and
  the server log has no `ERROR` or `WARN` line.

### Control: main without #27441 (2026-11-26, 2f4cede19d, the PR base)

`ShorthandIT` fails 12 of 14, with and without `uses-fix`. The other 19
tests pass. Only R2 and R3 pass, because they are refused on both builds.

- S1-S8: `executeJs(BoxJs.class)` is refused in Java:
  `com.acme.addon.ShorthandBox$BoxJs declares no JavaScript to run for
  append, setAttribute, getAttribute, hasAttribute, focus, toggleAttribute,
  toggleAttribute. Annotate every method with @JsExpression`. S7 fails too,
  although `mark` is annotated, since the whole interface is refused.
- S9: the typo is refused in Java the same way, so the browser is never
  reached.
- R1: refused, but with the old message, so the expected text does not match.
- The build logs `Leaving the JavaScript of ... out of the bundle` for
  `BoxJs`, `SubBoxJs`, `TypoJs` and `ReflectedShorthandJs`, so the bundle
  check fails (`this.append(...$0)` is missing).
- `/missing-shorthand`: the call fails in Java
  (`DefaultErrorHandler: Unexpected error: com.acme.addon.ReflectedShorthandJs
  declares no JavaScript to run for replaceChildren ...`), not with the
  #27078 bundle error, and the result text stays empty.

Not covered here: hotswap and the dev loop (see `repro/js-definition-hierarchy-hotswap`),
and a `@JsStringFallback` interface with a method without `@JsExpression`.

## Imports from modules (`@JsDefinitionImport`, vaadin/flow#27423)

`@JsDefinitionImport(from, defaultExport, namespace, exports)` on a
`JsInvokers` definition gives its JavaScript names from a module, like an
`import` statement. The generated function loads the module with `import()`
on the first call. These cases check the annotation in an add-on and in the
application, in the production bundle that `optimizeBundle` makes, under
strict CSP.

Add-on classes and modules:

- `ImportBox.ImportBoxJs`: imports `format` (default export) and `upper`
  from `acme-format.js` (in `META-INF/frontend`), and `acme-label.js` (in
  `META-INF/resources/frontend`) as the namespace `labels`. It also has a
  `@JsModule("./acme-eager.js")` and a method without `@JsExpression`.
  `ImportView` reaches it.
- `ImportBox.AltFormatJs`: the same JavaScript as `ImportBoxJs.showFormatted`
  (`this.textContent = format($0);`), with `format` from `acme-format-alt.js`.
- `UnusedDefinition.UnusedImportJs`: imports `acme-unused-import.js`. No
  route reaches it, so the function and the module may not be in the bundle.
- `ReflectedImportJs`: imports `acme-reflected-import.js` and is called only
  through reflection from `MissingImportView` (`/missing-import`). The
  `uses-fix` profile adds `@Uses(ReflectedImportJs.class)`.

The modules set `window.acmeFormatLoads`, `window.acmeLabelLoads` and
`window.appFormatLoads` when they load, so the test can see when, and how
often, each module is loaded.

`ImportView` (`/imports`), package `org.vaadin.example.imports`; the
application definitions are in `AppImports`, and the application module is
`app/src/main/frontend/app-format.js`:

| Row | Case | Expected |
| --- | --- | --- |
| I1 | default export and a named export: `upper(format($0))` | `[ONE]` |
| I2 | namespace, with the default export through it: `labels.default` | `label: two label-1 label-default` |
| I3 | `PendingJavaScriptResult` | `[three]` comes back |
| I4 | `JavaScriptResult<String>` | `label-1` comes back |
| I5 | variadic: `$0.map(format)` | `[a] [b] [c]` |
| I6 | a method without `@JsExpression` (`focus()`) in an interface with imports | the box has focus |
| I7 | `AltFormatJs`: same JavaScript, other imports | `<seven>`, while `ImportBoxJs` still gives `[seven]` |
| I8 | `AppFormatJs` (application) extends `ImportBoxJs` and imports another `format` from `app-format.js` | inherited method `[eight]` (the add-on's `format`), own method `{eight}` |
| I9 | `Page.executeJs(PageImportJs.class)`, module of the project | `NINE!` comes back |
| I10 | npm package: `from = "lit", exports = { "html", "render" }` | `<b class="lit">ten</b>` is rendered |
| I11 | `arguments` holds the imported values, as the Javadoc says | `3` (`format`, `upper`, `labels`) |
| I12 | `exports = "notExported"`, which the module does not have | fails in the browser, the error handler gets it |
| I13 | order of calls: `reset()`, `ConnectorJs.init()` (imports `init` from `app-connector.js`) and `record("after-init")` in one response; see [Review findings](#review-findings-on-vaadinflow27457-2026-11-27) | `data-order` is `init after-init:ready`. Fails on purpose |
| X1 | `JsSnippets` interface with an import | refused in Java |
| X2 | import that names nothing | refused in Java |
| X3 | `defaultExport = "default"` (reserved word) | refused in Java |
| X4 | `namespace = "$0"` (argument name) | refused in Java |
| X5 | `format` from two modules | refused in Java |
| X6 | bad import (`namespace = "class"`) on a parent interface that is not a definition | refused in Java, the message names the parent |

`AppImports.ImportOnClass` is a class with the annotation. It is not used
anywhere; the build only logs an error for it.

`ImportIT` checks each row, that the modules are loaded on the first call
and once (I1, I3 and I8 make three calls through two definitions that
import `acme-format.js`), that `acme-eager.js` is loaded with the view, what
the bundle carries, and the `/missing-import` case. It runs in the same
commands as `MissingDefinitionIT`:

```
rm -rf app/src/main/bundles
mvn install -DskipTests -pl addon
mvn clean -Pit verify -pl app
rm -rf app/src/main/bundles
mvn clean -Pit,uses-fix verify -pl app
```

### Results (2026-11-27, feat/js-definition-import b097481362, strict, production mode)

54 of 54 tests pass, with and without `uses-fix` (`AddonCspIT` 17,
`ShorthandIT` 14, `ImportIT` 21, `MissingDefinitionIT` 1,
`DirectFallbackIT` 1).

- I1-I11 work, with no browser console error. Each module is in a chunk of
  its own (`acme-format-*.js`, `acme-label-*.js`, `app-format-*.js`,
  `acme-format-alt-*.js`). None of them is loaded when the view opens, and
  each is loaded once after three calls. `acme-eager.js` (`@JsModule` on
  the same definition) is loaded with the view.
- The generated file imports the jar modules as
  `import("Frontend/generated/jar-resources/acme-format.js")`, the project
  module as `import("Frontend/app-format.js")` and the npm package as
  `import("lit")`.
- I12: the error handler gets `TypeError: t is not a function`. The
  production build minifies the names of the generated function, so the
  message does not name `notExported`. In development mode it does:
  `TypeError: notExported is not a function`. Nothing checks the export
  names at build time.
- X1-X6 are refused in Java with these messages:
  - X1: `... ImportSnippetsJs is annotated with @JsDefinitionImport, which
    only applies to an interface that extends JsInvokers. The function of a
    snippet is not asynchronous, so it can not wait for a module to load`
  - X2: `... NoNamesJs has @JsDefinitionImport(from = "./app-format.js")
    with no names to import. Declare a defaultExport, a namespace or
    exports, or use @JsModule to load the module for its side effects`
  - X3, X4: `... declares 'default' in @JsDefinitionImport(from =
    "./app-format.js"), which can not be imported as a name. Use a
    JavaScript identifier that is not a reserved word, not $0, $1, ...,
    which name the arguments, and not $executionContext. ...` (`'$0'` for X4)
  - X5: `... DuplicateNameJs declares 'format' more than once in
    @JsDefinitionImport. Import one of them as a namespace`
  - X6: `... InheritedBadImportJs extends ...BadImportParent, which declares
    'class' in @JsDefinitionImport(...) ...`
- The build logs `Leaving the JavaScript of ... out of the bundle` with the
  same message for X1-X6, and `Ignoring @JsDefinitionImport on
  org.vaadin.example.imports.AppImports$ImportOnClass, which only applies to
  an interface whose methods a definition extending JsInvokers declares`,
  and goes on.
- The bundle does not have `UnusedImportJs` or `acme-unused-import.js`.
- Without `uses-fix`: `/missing-import` does nothing, no browser console
  error, and the server logs one `ERROR` for two calls:
  `com.acme.addon.ReflectedImportJs declares JavaScript that was not
  included when creating the production bundle ... add a
  @Uses(ReflectedImportJs.class) ...`. With `uses-fix` the call works (the
  text from `acme-reflected-import.js` is shown), the module is in the build,
  and the server log has no `ERROR` or `WARN` line.

Development mode (`mvn spring-boot:run -pl app`, then the browser tests with
`mvn -Pit failsafe:integration-test -pl app '-Dit.test=ImportIT#i*+modules*+x*'`):
19 of 19 pass. At startup, Flow builds a new dev bundle ("Detected changed
content for frontend files that JavaScript definitions import").

### The bundle is rebuilt when an imported file changes

`mvn clean package -DskipTests -pl app`, run again after each change (the
bundle saved in `app/src/main/bundles` is kept between the runs):

| Change | Build log |
| --- | --- |
| none | `A production mode bundle build is not needed` |
| `app/src/main/frontend/app-format.js` (project file) | `Detected changed content for frontend files that JavaScript definitions import: - app-format.js`, then a new build |
| the same file changed back | the same, a new build |
| `acme-format.js` in the add-on jar (`META-INF/frontend`), `mvn install -pl addon` | `... - acme-format.js`, then a new build |
| `acme-label.js` in the add-on jar (`META-INF/resources/frontend`) | `... - acme-label.js`, then a new build; the new text is in the chunk |

### Two cases in profiles of their own

Each of these stops the build, so they are not in the default build.

- `-Pfallback-import` adds `FallbackImportJs`, a `@JsStringFallback`
  interface with `@JsDefinitionImport`. `build-frontend` fails:
  `Could not execute build-frontend goal: ...FallbackImportJs is annotated
  with @JsDefinitionImport, which does not apply to an interface annotated
  with @JsStringFallback: the string it stands in for can not name what a
  module exports`. This is how the build treats every `@JsStringFallback`
  interface that can not stand in for a string (#27128): the application
  owns it, so the build fails. The build first logs `Leaving the JavaScript
  of ... out of the bundle` with the same message.
- `-Pmissing-module` adds `MissingModuleView` (`/missing-module`), which a
  route reaches, with a definition that imports `./acme-does-not-exist.js`.
  The bundle check logs `No file found for 'acme-does-not-exist.js'`, then
  Vite fails: `[UNLOADABLE_DEPENDENCY] Could not load
  src/main/frontend/acme-does-not-exist.js`, and shows the generated line
  (`import("Frontend/acme-does-not-exist.js")`). The message does not name
  the Java interface. Unresolved modules are vaadin/flow#27424.

```
rm -rf app/src/main/bundles
mvn clean -Pfallback-import package -DskipTests -pl app
rm -rf app/src/main/bundles
mvn clean -Pmissing-module package -DskipTests -pl app
```

Not covered here: hotswap and the dev loop with imports.

### Review findings on vaadin/flow#27457 (2026-11-27)

These cases show the three findings of the review
(https://github.com/vaadin/flow/pull/27457#pullrequestreview-6087071461).
Flow `feat/js-definition-import` 48a0555cc6, strict, production mode.

**1. An import name and a `const` with the same name.** The `name-clash`
profile adds `NameClashView` (`/name-clash`). Its definition imports
`format` from `app-format.js`, and the method `local()` declares
`const format = ...`. `local()` does not use the import, and was valid
before the import was added. The server accepts the interface. Vite fails:

```
[PARSE_ERROR] Identifier `format` has already been declared
 627 │   return (async function (format) {
 628 │ const format = '<' + $0 + '>'; return format;
```

The message shows the generated line, but not the Java interface.

```
rm -rf app/src/main/bundles
mvn clean -Pname-clash package -DskipTests -pl app
```

Not checked: development mode, where the whole generated file should fail to
load, so that no definition works.

**2. Paths that rebuild the bundle on every build.** Each profile adds one
view with a definition that imports the path. The first build works (Vite
builds the import). The second build, with nothing changed, should log
`A production mode bundle build is not needed`, but it runs Vite again:

| Profile | `from` | Second build logs |
| --- | --- | --- |
| `rebuild-query` | `./app-style.css?inline` | `No file found for 'app-style.css?inline'`, then `Running Vite` |
| `rebuild-no-extension` | `./app-probe` (for `app-probe.js`) | `No file found for 'app-probe'`, then `Running Vite` |

```
rm -rf app/src/main/bundles
mvn clean -Prebuild-query package -DskipTests -pl app
mvn clean -Prebuild-query package -DskipTests -pl app
```

The same for `rebuild-no-extension` (delete `app/src/main/bundles` before
the first build).

**3. Order of calls (row I13).** In one response the server sends
`reset()`, then `ConnectorJs.init()`, which imports `init`, and
`record("after-init")`, which has no import. `init` sets `$connector` on the
element, as in the example of the `@JsDefinitionImport` Javadoc
(`connector.initLazy(this)`). Each step is added to `data-order`.
`ImportIT.i13_callsRunInTheOrderTheyWereSent` expects
`init after-init:ready` and fails on purpose:

```
I13: first click 'after-init:missing init', second click 'after-init:missing init'
```

`record` runs before `init`, also on the second click, when the module is
already loaded. `mvn clean -Pit verify -pl app`: 55 tests, 54 pass, and
`i13_callsRunInTheOrderTheyWereSent` fails. The `uses-fix` run was not
repeated.

#### Retest on the fixes (2026-11-27, 9b003631c1)

The PR fixed findings 1 and 2 (b1d8647621, 9b003631c1) and documented
finding 3 in the Javadoc. Two browser tests were added for the profiles:
`NameClashIT` runs with `-Pit,name-clash`, `RebuildPathIT` with
`-Pit,rebuild-*`. Each one is skipped without its profile.

- **1. Name clash:** `mvn clean -Pit,name-clash verify -pl app` builds.
  `NameClashIT` passes: `local()` returns `<x>` (its `const format` hides the
  import) and `formatted()` still returns `{y}`, with no browser console
  error.
- **2. Rebuild:** fixed. For each profile, the IT build, then a build with no
  change, then a build after a line is added to the imported file:

  | Profile | Call in the browser | No change | File changed |
  | --- | --- | --- | --- |
  | `rebuild-query` | `.app-style{color:green}` | `A production mode bundle build is not needed` | `Detected changed content for frontend files that JavaScript definitions import: - app-style.css`, new build |
  | `rebuild-no-extension` | `probe x` | not needed | `Detected changed frontend files: - app-probe.js`, new build |

  The Vite config of Flow gives a CSS file imported with `?inline` as a Lit
  `CSSResult`, so `RebuildQueryView` reads `cssText`.
- **3. Order of calls:** not changed in the client, so
  `i13_callsRunInTheOrderTheyWereSent` still fails on purpose
  (`after-init:missing init` on both clicks). The Javadoc now says that the
  JavaScript of an importing call runs after the calls that follow it in the
  same round trip.

`mvn clean -Pit verify -pl app`: 57 tests, 54 pass, 2 skipped (the profile
tests), I13 fails. The same with each profile, where its own test runs and
passes.

## Notes

- Calls that run a fallback in place of a string do not go through
  `JsDefinitionProxy`, so the runtime check of #27078 does not cover them.
  That is fine while #27077 always keeps the fallbacks. #27078 says so in
  its documentation (2dbc299775).
- Merging #27077 into #27078 broke a test of #27077 (`BundleValidationTest`
  used `BundleValidationUtil.JS_DEFINITION_FUNCTION_IDS_STATS_KEY`, which
  #27078 moved to `BundleUtils`). Fixed in #27078 (a516008ac1).

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
