# Reproducer: shortcuts as declared JavaScript

Flow branch: `declare-shortcut-javascript` ("declare shortcut JavaScript
instead of sending it as strings").

Before the change, a shortcut sent its listener filter and its keydown
delegate to the browser as JavaScript strings. A Content Security Policy
without `unsafe-eval` blocks them. With the change, the shortcut calls
declared JavaScript (`ShortcutFilterJs`, `ShortcutJs`) and sends the keys,
modifiers and options as data. `Shortcuts.setShortcutListenOnElement` gets
an overload that takes a declared `JsFunction` locator. The `String`
overload is deprecated.

The views use only Flow HTML components (no Vaadin components), so the CSP
log shows only the shortcut call sites. Popovers open with
`popovertarget`, so the views send no JavaScript of their own.

## Setup

1. Install the Flow branch (version `25.4-SNAPSHOT`), including
   `flow-client`:

   ```
   mvn install -DskipTests -pl flow-server,flow-build-tools,flow-plugins/flow-plugin-base,flow-plugins/flow-maven-plugin,flow-client,vaadin-spring -am
   ```

   For the baseline, install `main` in the same way.
2. Delete `src/main/bundles`, `src/main/frontend/generated` and `target`
   when you switch between the baseline and the branch.
3. Run the app in one of three modes:
   - warn (default): `./mvnw spring-boot:run`. Each string JavaScript call
     site is logged once by `CspViolationReporter`, with a stack trace.
   - strict, development mode:
     `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--vaadin.csp=strict"`.
     A string JavaScript call site throws `CspViolationException`. No CSP
     header is sent.
   - strict, production mode: `./mvnw package -DskipTests`, then
     `java -jar target/spring-skeleton-1.0-SNAPSHOT.jar --vaadin.csp=strict`.
     Vaadin also sends a `Content-Security-Policy` header without
     `unsafe-eval`, so the browser enforces the policy.

## Cases

Each view has a log of the shortcuts that fired on the server, as
`number-name`.

| View | Case | Expected |
|---|---|---|
| `/shortcuts` | A. `Shortcuts.addShortcutListener(view, .., S, CONTROL)` | Ctrl+S logs `ctrl-s`, and the browser does not open its save dialog. Ctrl+Shift+S and S log nothing. Not a click shortcut: `addClickShortcut` calls `allowBrowserDefault()`, so with it the save dialog opens too (also on `main`) |
| `/shortcuts` | B. UI-owned shortcut (no owner selector) | F2 logs `ui-f2` |
| `/shortcuts` | C. Alt shortcut (AltGraph not forbidden) | Alt+K logs `alt-k` |
| `/shortcuts` | D. `setBrowserDefaultAllowed`, changed on an active shortcut | Typing 1 in the field logs `digit-1`. The 1 is typed only when the toggle is on |
| `/shortcuts` | E. `setEventPropagationAllowed`, changed on an active shortcut | Typing 2 in the field logs `inner-2`. It also logs `outer-2` only when the toggle is on |
| `/shortcuts` | F. `setResetFocusOnActiveElement`, changed on an active shortcut | Type a name, press Ctrl+Enter in the field. On: logs `greet:<name>`. Off: logs the previous value |
| `/popover` | Owner scope guard (#24974) | Popover closed, page input: Alt+G, Alt+H, F4, Alt+I all log. Popover open, page input: Alt+G, Alt+H, F4 log. Popover open, popover input: Alt+H (`allowEventsFromNestedModals`) and Alt+I log |
| `/delegate` | Keydown delegate, declared locator | Overlay input: Alt+D logs `delegate-a` and `delegate-b` once each (one delegate for the locator set twice), Alt+E logs `nested-e`. Nested popover input: only Alt+E logs. Page input: nothing |
| `/string-locator` | Keydown delegate, deprecated `String` locator | Same as `/delegate` in warn mode. Fails in strict mode, also with the branch |

`/delegate` calls the new overload, which `main` does not have. To run the
app against `main`, delete `DelegateView.java` and its link in `MainView`.

## Results (2026-11-10)

Baseline: `main` at 74bfef5b98 (merge base). Branch: a530a7a8f9. Checked in
headless Chrome.

| Mode | Baseline | Branch |
|---|---|---|
| warn | All cases as expected (without `/delegate`). Logged: `DomListenerRegistration.setFilter` and `Element.executeJs` from `ShortcutRegistration` | All cases as expected. Logged: only `JsFunction.of` from `ShortcutRegistration.toLocatorFunction` (the `/string-locator` view) |
| strict, development | `/shortcuts` fails to open (`CspViolationException`) | All cases as expected, except `/string-locator`, which fails to open |
| strict, production | not run | Same as strict development. The browser reports no CSP violation |

Side note: the Javadoc of `CspMode.STRICT` still says that the event filters
of keyboard shortcuts need `eval`.

## Review findings (reproducers)

Tests in `src/test/java/org/vaadin/example/review`. Run them against the
installed Flow branch with:

```
./mvnw test -Dtest='org.vaadin.example.review.*Test'
```

Each test passes when the problem is fixed. `DuplicateDelegateTest` and
`DelegateOptionsTest` use the new overload, so to run the others against
`main`, set them aside together with `DelegateView` (see above).

Checked 2026-11-10 against the branch (a530a7a8f9) and `main` (74bfef5b98):

| Test / view | Finding | Branch | `main` |
|---|---|---|---|
| `FireShortcutTest` | browserless-test `fireShortcut` reads `ShortcutRegistration.generateEventKeyFilter` / `generateEventModifierFilter` by reflection and matches the filter as a string | `ExceptionInInitializerError` caused by `NoSuchMethodException` | passes |
| `NullLocatorCompileTest` | `setShortcutListenOnElement(null, component)` is ambiguous with the new `JsFunction` overload | javac: "reference to setShortcutListenOnElement is ambiguous" | compiles |
| `DuplicateDelegateTest` | a variadic declared locator, or a `JsFunction.of` locator, set again in a later round trip registers a second delegate | 2 delegates (keydown relayed twice, shortcut fires twice); a single-`String` declared locator gets 1 | n/a (no overload) |
| `DelegateOptionsTest` | changing `setBrowserDefaultAllowed` on an active shortcut does not update the delegate | no new delegate; the browser keeps preventing the default | same bug (not run, code unchanged in behavior) |
| `/many-shortcuts` | every keydown evaluates every shortcut filter snippet again | 0.165 ms per non-matching keydown with 300 shortcuts | 0.06 ms. Slower, but not a real cost |
