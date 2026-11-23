# JavaScript definition hierarchy and hotswap (vaadin/flow#27152)

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

The root `pom.xml` imports `flow-bom` 25.4-SNAPSHOT above `vaadin-bom`.
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

Run `.vaadin/vaadin-dev shutdown` when done, and remove `.vaadin`, `.agents`
and `.claude`.
