# JavaScript definition hierarchy and hotswap (vaadin/flow#27152, #27441)

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

Run `.vaadin/vaadin-dev shutdown` when done, and remove `.vaadin`, `.agents`
and `.claude`.
