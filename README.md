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

## Results (2026-11-23, PR branch at c2b211430a)

| Changed | What `apply` does | What runs after a reload |
|---|---|---|
| `PlainParentJs` | redefines without a restart, reports it as live | **old JavaScript**; `src/main/frontend/generated/vaadin-js-definitions.js` keeps the old function; nothing is logged |
| `AnnotatedParentJs` | restarts ("frontend imports changed") | new JavaScript |
| `DirectJs` | restarts ("frontend imports changed") | new JavaScript |

`JsDefinitionHotswapper` and `DevLoopRedefiner` only handle changed classes
that have `@JsDefinition`. `PlainParentJs` has none, and
`PlainParentChildJs` did not change, so nothing is regenerated and nothing
restarts.

Run `.vaadin/vaadin-dev shutdown` when done, and remove `.vaadin`, `.agents`
and `.claude`.
