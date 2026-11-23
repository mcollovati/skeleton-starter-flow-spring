# Errors in declared JavaScript that nobody subscribes to

Cases for vaadin/flow#27025 (fixes vaadin/flow#27024). The `/declared-js-errors`
view (`DeclaredJsErrorsView`) runs declared JavaScript (`@JsDefinition`) that
fails, with `vaadin.csp=strict`.

The build generates every definition that a call runs as an `async function`,
so an error in it rejects a promise instead of throwing. Before the fix, the
client handled that promise only when the server subscribed to the result, so
a failing call without a subscriber became an unhandled promise rejection and
Flow did not report it. With the fix, the client reports it the same way as a
failed `executeJs(String)`: `Console.reportStacktrace`, which throws the error
again outside the call, so the page gets an uncaught `error` event, plus
`Console.error("Exception is thrown while running <name>. ...")`, which only
shows outside production mode.

`DeclaredJsErrorsIT` records both kinds of events in the page
(`window.__uncaught` for `error`, `window.__unhandledRejections` for
`unhandledrejection`). A case counts as reported when the error is in
`__uncaught` and not in `__unhandledRejections`. Playwright's `pageerror` is
not enough: Chrome fires it for both.

## What it needs

Flow from the PR branch `fix/log-declared-js-rejections` installed as
25.4-SNAPSHOT: `flow-server`, `flow-client`, `flow-build-tools`,
`flow-maven-plugin`, `vaadin-spring`, `vaadin-dev-server`:

```
git fetch origin pull/27025/head:pr-27025
git switch --detach pr-27025
mvn install -DskipTests -pl flow-server,flow-client,flow-build-tools,flow-plugins/flow-maven-plugin,vaadin-spring,vaadin-dev-server -am
```

The other 25.4-SNAPSHOT artifacts (components, BOM) must already be in the
local repository. Build offline (`-o`), so Maven does not replace the local
snapshots with remote ones.

Run the ITs, which build and start the application in production mode:

```
mvn -o clean verify -Pit -Dvaadin.force.production.build=true
```

`-Dvaadin.force.production.build=true` matters when you switch between Flow
builds (for example the PR head and its base): the build stores the compiled
bundle, with the Flow client in it, in `src/main/bundles/`, and without the
flag a later build reuses it. Deleting `src/main/bundles/` has the same effect.

Other options: `-Dspring-boot.run.jvmArguments=-Dserver.port=<port>` with
`-Dit.baseUrl=http://localhost:<port>` for another port, `-Dit.jmxPort=<port>`
when the JMX port 9001 is taken.

For the `executeJs(String)` case, start the application with
`vaadin.csp=off` and tell the test so:
`-Dspring-boot.run.jvmArguments=-Dvaadin.csp=off -Dit.csp=off`.

## Cases

| Case | Button | What it checks | Expected with the fix |
|------|--------|----------------|-----------------------|
| C1 | `#void-throws` | declared `void` method throws | uncaught error `void-throws`, no unhandled rejection |
| C2 | `#unsubscribed-throws` | method returns `PendingJavaScriptResult`, throws, the result is not used | uncaught error `unsubscribed-throws`, no unhandled rejection |
| C3 | `#async-rejects` | declared `void` method returns a promise that rejects later | uncaught error `async-rejects`, no unhandled rejection |
| C4 | `#subscribed-throws` | method throws, the result is used with `then(value, error)` | the error handler gets `Error: subscribed-throws`; nothing is reported in the browser (the fix leaves a subscribed rejection to the server) |
| C5 | `#succeeds` | method returns 42, the result is used | `value: 42`, nothing reported |
| C6 | `#string-throws` | `Page.executeJs(String)` that throws, for comparison | strict: the server refuses it (`CspViolationException`); `csp=off`: uncaught error `string-throws`, no unhandled rejection |

## Results

Production mode, `mvn -o clean verify -Pit -Dvaadin.force.production.build=true`,
2026-11-16:

| Case | Base 1bd863e325 (merge base with main) | PR head c1dedf8924 |
|------|------|------|
| C1 | fails: unhandled rejection `void-throws`, no uncaught error | passes |
| C2 | fails: unhandled rejection `unsubscribed-throws` | passes |
| C3 | fails: unhandled rejection `async-rejects` | passes |
| C4 | passes | passes |
| C5 | passes | passes |
| C6, strict | passes (refused) | passes (refused) |
| C6, `csp=off` | not run | passes (uncaught error) |

In production mode Flow logs nothing to the browser console in either version
(`Console.error` is off there), so the uncaught error from
`Console.reportStacktrace` is the only signal.

Not checked: development mode, where the fix also logs
`Exception is thrown while running <name>`.
