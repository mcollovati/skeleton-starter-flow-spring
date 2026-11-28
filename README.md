# JavaScript definition hierarchy and hotswap (vaadin/flow#27152, #27441, #27423, #27425)

With vaadin/flow#27152, a JavaScript definition has `@JsDefinition` and
extends `JsInvokers` or `JsSnippets`. This project checks what hotswap does
when a definition inherits its method from a parent interface, and the
parent changes.

| Interface | Parent | Button |
|---|---|---|
| `PlainParentChildJs` (`@JsDefinition`) | `PlainParentJs`, no `@JsDefinition` | "Run PlainParentChildJs.show" |
| `AnnotatedParentChildJs` (`@JsDefinition`) | `AnnotatedParentJs`, `@JsDefinition` | "Run AnnotatedParentChildJs.show" |
| `DirectJs` (`@JsDefinition`), control | none | "Run DirectJs.show" |

Each button runs the inherited `show` method, which writes the version of
its JavaScript (`v1`) next to the button.

The view uses only Flow HTML components. #27152 changes the erased signature
of `executeJs(Class)`, so the Vaadin components of the release do not work
with the Flow of the branch. Their definitions are logged as "Leaving the
JavaScript of ... out of the bundle", which is expected here.

## Setup

The root `pom.xml` imports `flow-bom` 25.5-SNAPSHOT above `vaadin-bom`
(25.4-SNAPSHOT for the #27152 results below).
Install the Flow modules of the PR branch first, from the Flow checkout:

```
mvn install -DskipTests -pl flow-bom,vaadin-spring,vaadin-dev-server,flow-html-components,flow-dnd,flow-polymer-template,flow-lit-template,flow-react,flow-push,flow-webpush,flow-data,flow-client,flow-devloop-daemon -am
```

Then, in this project:

```
mvn -nsu vaadin:install-dev-cli
VAADIN_DEV_DAEMON_OPTS="-Dvaadin.frontend.hotdeploy=true" .vaadin/vaadin-dev start
```

Open http://localhost:8097/ and press the three buttons: each shows `v1`.

## Steps

For each interface, change `v1` to `v2` in its `@JsExpression`, run
`.vaadin/vaadin-dev apply`, reload the page and press the button again.

To test a redefine without a restart, which is the same path as hotswap
from an IDE, change the version again, run
`.vaadin/vaadin-dev apply --no-restart` and then `.vaadin/vaadin-dev apply`.
The first command only compiles. The second one then only redefines the
class.

## Results

### PR branch at e114017dbd (2026-11-23): all fixed

| Changed | Plain `apply` | Redefine without a restart |
|---|---|---|
| `PlainParentJs` | restarts ("frontend imports changed"), new JavaScript | file written again, Vite HMR update, new JavaScript |
| `AnnotatedParentJs` | restarts, new JavaScript | not tested |
| `DirectJs` | restarts, new JavaScript | file written again, Vite HMR update, new JavaScript |

After a redefine, `vaadin-js-definitions.js` keeps the old function next
to the new one. This is on purpose: a browser that has the file may still
call it, and the next build removes it.

### Earlier results

- c2b211430a: changing `PlainParentJs` was redefined without a restart and
  reported as live, but the old JavaScript kept running and nothing was
  logged. `JsDefinitionHotswapper` and `DevLoopRedefiner` only handled
  changed classes with `@JsDefinition`. Fixed in faa7a182ed and cbcaa294ab.
- 4927686f45: a plain `apply` restarted and worked, but a redefine without a
  restart still ran the old JavaScript. `Class.getMethods()` of
  `PlainParentChildJs` kept the methods from before the parent was
  redefined, with the old `@JsExpression`. Fixed in 2b814554fc.

## Methods without `@JsExpression` (vaadin/flow#27441)

With #27441, a method of an interface that extends `JsInvokers` can leave
out `@JsExpression` to call the element method of the same name:
`void append(String text);` runs `return this.append($0);`. These cases
check hotswap with the dev loop. Each button first empties its text, then
runs the call, so the text shows which JavaScript ran.

| Case | Interface | Button | Change |
|---|---|---|---|
| H1 | `ShorthandDirectJs` (`@JsDefinition`) | "Run ShorthandDirectJs.append" | add `@JsExpression("this.textContent = 'direct v2: ' + $0")`, then remove it again |
| H2 | `ShorthandParentChildJs` (`@JsDefinition`), method in `ShorthandParentJs` (no `@JsDefinition`, only a shorthand method) | "Run ShorthandParentChildJs.append" | the same, in the parent (`'parent v2: '`) |
| H3 | `PageTitleJs`, called through `Page.executeJs(Class)` | "Run Page.executeJs(PageTitleJs).setTitle" | remove `@JsExpression("document.title = $0")`: the page call must be refused; then add it back |
| H4 | `ShorthandParamsJs` | "Run ShorthandParamsJs.append" | `append(String text)` to `append(String first, String second)`, and the call in `MainView` to `.append("clicked", " twice")` |

H2 is what `JsDefinitionHotswapper` changes in #27441: before, an interface
counted as declaring JavaScript only if a method had `@JsExpression`, so a
parent with only shorthand methods was not seen. H4 is what
`DevLoopRedefiner` changes: the parameters of a shorthand method are part of
the frontend fingerprint. H3 checks that the cache behind the page check is
cleared before a hotswap.

For each change run `.vaadin/vaadin-dev apply --no-restart` (a pure
redefine, as from an IDE), press the button again, then
`.vaadin/vaadin-dev apply`.

### Results: #27441 e04fb83372 (2026-11-26)

Dev loop with `-Dvaadin.frontend.hotdeploy=true`. Flow modules of the PR
installed in a private local repository (chained with
`maven.repo.local.tail`); the Maven wrapper of this project (3.8.4) does not
support that, so the dev loop ran with `-Dvaadin.dev.maven=<Maven 3.9>`.

| Case | `apply --no-restart` | then `apply` |
|---|---|---|
| H1 add `@JsExpression` | "frontend imports changed (ShorthandDirectJs)", redefined, file rewritten, Vite HMR: `direct v2: clicked` | `redefineClasses(1)`, same |
| H1 remove it | same message: `clicked` (the old shorthand function was still in the file) | same |
| H2 add `@JsExpression` in the parent | "frontend imports changed (ShorthandParentJs)", file rewritten: `parent v2: clicked` | same |
| H2 remove it | `clicked` | same |
| H3 remove `@JsExpression` | refused: `org.vaadin.example.PageTitleJs has setTitle without @JsExpression, which calls the method of the same name on the element the call runs on. A call through the page runs on no element, ...` | same |
| H3 add it back | `called`, title `title set` | same |
| H4, JDK 25 (no JBR) | "redefine rejected: attempted to delete a method" | restart: `clicked twice`, the file has `return this.append($0, $1);` |
| H4, JBR 21.0.11 | "frontend imports changed (ShorthandParamsJs)", the file has `return this.append($0, $1);`, but the button fails: `AbstractMethodError: Receiver class jdk.proxy2.$Proxy117 does not define ... append(String, String)` | same error, no restart |
| H4, JBR + `--add-opens java.base/jdk.internal.loader=ALL-UNNAMED` | `clicked twice` | not needed |

All #27441 parts work. The H4 failure on the JBR is not caused by #27441:
the JDK proxy class that `executeJs(Class)` returns keeps the old methods.
HotswapAgent's `ProxyPlugin` would regenerate it, but fails with
`InaccessibleObjectException: ... module java.base does not "exports
jdk.internal.loader" to unnamed module`. The dev loop opens
`jdk.internal.loader` only for the embedded Maven goal runtime
(`MavenGoalRuntime.extraJvmFlags`), not when it launches the main class
(`Launch`, which opens `java.lang`, `java.lang.reflect`, `java.io`,
`java.util` and `java.beans`). So any change to the methods of a
`JsInvokers` interface, with or without `@JsExpression`, should fail the
same way on a JBR. Use
`.vaadin/vaadin-dev start --jvm-args "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED"`
until the dev loop opens it.

The `-javaagent` of this run was a `flow-devloop-daemon` 25.5-SNAPSHOT from
`~/.m2`, not from the PR: the CLI resolves it with the project's `mvnw` (Maven
3.8.4), which ignores the private repository. `flow-server`, `vaadin-dev-server` and `flow-build-tools`
on the application classpath were the PR's.

## Imports from modules (`@JsDefinitionImport`, vaadin/flow#27423)

`@JsDefinitionImport(from, defaultExport, namespace, exports)` gives the
JavaScript of a `JsInvokers` definition names from a module. The imports are
hashed into the identifier of each function, so a changed import is a new
function, as a changed expression is. These cases check that a hotswap with
the dev loop picks up a changed import, and that an edited module reaches
the browser. The definitions are in `org.vaadin.example.imports`, the
modules are `src/main/frontend/format-v1.js`, `format-v2.js` (same exports,
other text) and `src/main/resources/META-INF/resources/frontend/res-format.js`
(a frontend file on the class path, which the build imports from
`generated/jar-resources`, as one in a jar).

| Case | Interface | Button | Change |
|---|---|---|---|
| M1 | `ImportDirectJs` | "Run ImportDirectJs.show" | only the module: `from = "./format-v1.js"` to `"./format-v2.js"`, then back |
| M2 | `ImportParentChildJs` (`@JsDefinition`), method and import in `ImportParentJs` (no `@JsDefinition`) | "Run ImportParentChildJs.show" | the same, in the parent |
| M3 | `ImportAddJs`, no import | "Run ImportAddJs.show" | add `@JsDefinitionImport(from = "./format-v1.js", exports = "shout")` and use `shout($0)`, then take both away |
| M4 | `ImportResourceJs`, module on the class path | "Run ImportResourceJs.show" | `namespace = "res"` to `"other"`, expression `'other ' + other.format($0)`, then back |
| M5 | none | "Run ImportDirectJs.show" or "Run ImportParentChildJs.show" | edit `format-v1.js` itself (`'v1('` to `'v1-edited('`), then back |
| M6 | `ImportDirectJs` | "Run ImportDirectJs.show" | `defaultExport = "format"` to `"default"`, which is refused, then back |

M2 checks that the import of a parent without `@JsDefinition` reaches the
definitions that extend it. M4 checks that the hotswap imports the module
from where the build does: the hotswapper got a class finder in #27423 to
tell a file of the frontend folder from one on the class path.

For M1-M4 and M6 run `.vaadin/vaadin-dev apply --no-restart`, press the
button, then `.vaadin/vaadin-dev apply` and press it again. For M5 keep the
page open, press the button, edit the file, press it again.

### Results: feat/js-definition-import b097481362 (2026-11-27)

JDK 25 (no JBR; no case changes a method signature). Flow modules of the
branch in a private local repository (`MAVEN_ARGS` with
`maven.repo.local.tail`, Maven 3.9 through `-Dvaadin.dev.maven`), and the
daemon jar built from the branch through `-Dvaadin.dev.daemonJar`.

With `-Dvaadin.frontend.hotdeploy=true` (Vite running):

| Case | `apply --no-restart` | then `apply` |
|---|---|---|
| M1 to v2 | "frontend imports changed (ImportDirectJs)", redefined, file rewritten (new function with `import("Frontend/format-v2.js")`), Vite HMR: `v2(clicked)` | `redefineClasses(1)`: `v2(clicked)` |
| M1 back | `v1(clicked)` | same |
| M2 to v2 | "frontend imports changed (ImportParentJs)", HMR: `parent v2(clicked)` | same |
| M2 back | `parent v1(clicked)` | same |
| M3 add | "frontend imports changed (ImportAddJs)", HMR: `v1 CLICKED!` | same |
| M3 remove | `no import: clicked` | same |
| M4 | new function with `import("Frontend/generated/jar-resources/res-format.js")`, as the build writes it: `other res(clicked)` | same |
| M4 back | `res(clicked)` | same |
| M6 `"default"` | the call is refused in Java: `ImportDirectJs declares 'default' in @JsDefinitionImport(from = "./format-v1.js"), which can not be imported as a name. ...`; the log has `Leaving the JavaScript of ...ImportDirectJs out of the bundle` with the same text | same |
| M6 back | `v1(clicked)` | same |

With the page kept open (no reload): M1 with `apply --no-restart` changes
the result from `v1(clicked)` to `v2(clicked)`. M5: Vite sends the change of
`format-v1.js` up to `vaadin-js-definitions.js` ("hot updated:
/generated/vaadin-js-definitions.js"), and the next press shows
`v1-edited(clicked)`. Putting the file back works the same way.

Without hot deploy (dev bundle):

- M1 with `apply --no-restart`: the hotswapper warns `The JavaScript declared
  by org.vaadin.example.imports.ImportDirectJs is not the JavaScript the
  frontend bundle carries. ... a call made through the definition keeps
  running the previous version, or finds no function at all, until the
  application is restarted.` The button does nothing and the browser console
  shows `No JavaScript in the bundle for 5613870b...`: with another import
  the function has another identifier, so there is no previous version to
  run. The `apply` after it only redefines, so the call still fails.
  `vaadin-dev restart` logs `Detected changed content for frontend files
  that JavaScript definitions import: - format-v2.js`, builds a new dev
  bundle, and the button shows `v2(clicked)`. Changing it back with a plain
  `apply` restarts ("frontend imports changed (ImportDirectJs)") and works.
- M5: a plain `apply` after editing `format-v1.js` restarts ("frontend
  changed (dev bundle rebuild)") and the button shows `parent
  v1-edited(clicked)`. Putting it back works the same way.

The other buttons (#27152, #27441) still work at the end.

Notes:

- The first page after a new dev bundle logs a 404 for `/favicon.ico`.
  This is not related to the cases.
- The dev loop kept the launch classpath of an earlier run in
  `target/devloop` (a private repository that no longer existed), although
  `target/devloop/cp.txt` had been resolved again, and the app failed with
  `NoClassDefFoundError: com/vaadin/flow/component/page/AppShellConfigurator`.
  `vaadin-dev shutdown` and deleting `target/devloop` fixed it.
- The startup log warns that `META-INF/resources/frontend` is deprecated
  (use `META-INF/frontend`), but the `@JsDefinitionImport` Javadoc names
  only `META-INF/resources/frontend` for a jar.

Not covered here: a module in a separate jar (the class-path file of M4 takes
the same path), and the JBR with a changed method signature (see H4).

Run `.vaadin/vaadin-dev shutdown` when done, and remove `.vaadin`, `.agents`
and `.claude`.

## Module definitions (`JsModuleInvokers`, vaadin/flow#27425)

vaadin/flow#27425 adds `JsModuleInvokers` and `Page.executeModuleJs`: the
methods of a module definition run with one module as `this`. The module is
loaded with the route, or at startup in development mode: the imports file
imports it and registers it in `window.Vaadin.Flow.jsDefinitionModules`. The
generated function takes it from there, so the call does not wait for
`import()` and runs before the calls that follow it. When the module is not
registered, the function loads it with `import()`, warns once in the
browser, and the call runs after the calls that follow it. These cases check
what a hotswap does to the registration.

The definition is `org.vaadin.example.modules.ConnectorJs`. Its module,
`src/main/frontend/connector-v1.js`, sets `$connector` with a version on the
element; `connector-v2.js` is the same with `v2`. Each button makes a new
element, calls the module and then `StatusJs.show("after")` in the same
round trip. The text is `<version> after`, or `missing after` when the
module call ran late.

| Case | Button | Change |
|---|---|---|
| J1 | "Run ConnectorJs.initLazy" | the module: `from = "./connector-v1.js"` to `"./connector-v2.js"`, then back |
| J2 | "Run ConnectorJs.initLazyTagged" | only the expression: `' expr-v1'` to `' expr-v2'` |
| J3 | "Run ConnectorJs.initLazy" | edit the module file: `version: 'v1'` to `'v1-edited'` |
| J4 | "Run ConnectorAddedJs.initLazy" | add a new definition (below); before that the button shows `refused: no class ConnectorAddedJs yet` |

J4 adds `src/main/java/org/vaadin/example/modules/ConnectorAddedJs.java`:

```java
@JsDefinition
@JsDefinitionImport(from = "./connector-v2.js")
public interface ConnectorAddedJs extends JsModuleInvokers {
    void initLazy(Element element);
}
```

To check that the registry was used, run this in the browser console:
`Object.keys(window.Vaadin.Flow.jsDefinitionModules)`.

### Results: vaadin/flow#27493 73da7a8d1f (2026-11-28)

JDK 25. Flow modules of the PR in a private local repository (`MAVEN_ARGS`
with `maven.repo.local.tail`), daemon jar of the PR through
`-Dvaadin.dev.daemonJar`. The page was driven with Playwright.

Before any change: the registry has `Frontend/connector-v1.js`, the buttons
show `v1 after` and `v1 expr-v1 after`.

With `-Dvaadin.frontend.hotdeploy=true` (Vite running):

| Case | Result |
|---|---|
| J1 `apply --no-restart` | The dev loop says "frontend imports changed (ConnectorJs) ... escalating to restart", but only redefines. The registry still has only `connector-v1.js`. On each new page the first call loads `connector-v2.js` itself, warns `Loading Frontend/connector-v2.js for a call, since no view shown so far loaded it. That call runs after the calls that follow it.` and shows `missing after`. The next calls show `v2 after`. A plain `apply` after it only redefines again, with the same result |
| J1 `restart` | the registry has `connector-v2.js`, `v2 after` with no warning |
| J1 back, plain `apply` | restarts ("frontend imports changed (ConnectorJs)"), the registry has `connector-v1.js`, `v1 after` |
| J2 `apply --no-restart`, page open | the next press shows `v1 expr-v2 after`, also after a reload; no warning (the module did not change). The dev loop asks for a restart, as it does for any changed expression |
| J3, page open | Vite reloads the whole page (`hmr invalidate /generated/index.tsx`, `page reload`). Then `v1-edited after`, registered, no warning |
| J4 `apply --no-restart` | the new class works, but through the fallback: the first call shows `missing after` with the warning for `connector-v2.js`, the next ones `v2 after` |
| J4 `restart` | both modules are registered, `v2 after` with no warning |

Without hot deploy (dev bundle):

| Case | Result |
|---|---|
| J1 `apply --no-restart` | the hotswapper warns `The JavaScript declared by org.vaadin.example.modules.ConnectorJs is not the JavaScript the frontend bundle carries ...`; the browser logs `No JavaScript in the bundle for 8d5e58c5...` and shows `missing after`. Same as M1 |
| J1 `restart` | `Detected JavaScript that the bundle does not carry, declared by: org.vaadin.example.modules.ConnectorJs`, a new dev bundle, the registry has `connector-v2.js`, `v2 after` |
| J3, plain `apply` | restarts ("frontend changed (dev bundle rebuild)"), `v2-edited after` |

At the end all the other buttons (#27152, #27441, #27423) still work.

Summary: a pure redefine (the IDE hotswap path) of a changed module, or of
a new module definition, does not update the registration. The calls work,
but the first one on each page runs late and warns, until a restart. The
dev loop restarts by itself for a plain `apply`, but not for an `apply`
after `apply --no-restart`. An edit of the module file reloads the page
(hot deploy) or restarts (dev bundle), and both register the new code. This
matches the "Known limits" of the issue.

Notes:

- The log has `ERROR ... Leaving the JavaScript of
  com.vaadin.flow.data.renderer.LitRendererJs out of the bundle: ... extends
  none of JsInvokers, JsModuleInvokers and JsSnippets`. The class is in
  `vaadin-renderer-flow` 25.4.0-beta2, built before the marker interfaces.
  Not related to the cases.

Not covered here: a module in a jar, and a module definition in a parent
interface.

Run `.vaadin/vaadin-dev shutdown` when done, and remove `.vaadin`, `.agents`
and `.claude`.
