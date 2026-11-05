# Reproducer for vaadin/flow#26699

PR: "declare simple executeJs call sites as JavaScript definitions".

The app runs with `vaadin.csp=warn`. In this mode Vaadin logs a
`CspViolationReporter` warning, with a stack trace, the first time each call
site sends JavaScript as a string. The PR changes four call sites. With the PR,
they must work as before and must not be logged.

## Setup

1. Install the PR branch of Flow (version `25.4-SNAPSHOT`), including
   `flow-client`:

   ```
   mvn install -DskipTests -pl flow-server,flow-build-tools,flow-plugins/flow-plugin-base,flow-plugins/flow-maven-plugin,flow-client,vaadin-spring -am
   ```

   For the baseline, install `main` in the same way.
2. Run the app: `./mvnw spring-boot:run`. Log in with `user` / `user`.
3. To use the Flow client router instead of the React router, run
   `./mvnw spring-boot:run -Dvaadin.react.enable=false -Dspring-boot.run.jvmArguments=-Dvaadin.react.enable=false`.
   Before you switch router mode, delete `src/main/frontend/generated`,
   `src/main/bundles` and `target`. Otherwise a stale `index.ts` from the other
   mode breaks the dev bundle build (`Cannot find module '@vaadin/router'`).

## Cases

| View | Changed code | Expected |
|---|---|---|
| `/resize` | `Page.ensureResizeListener` uses `ElementJs.forwardWindowResize()` | Resize the window: the size follows the viewport |
| `/flash` | `Element.flashClass` uses `ElementJs.flashClass(..)` | Click Flash: the box is orange for 1s, then the `flash` class is removed |
| `/logout`, Logout | `AuthenticationContext.logout` round trip (push `WEBSOCKET`) uses `PageJs.getHref()` | Ends on `/login` |
| `/logout?transport=long-polling`, Logout from a background thread | Same round trip, background thread branch | Ends on `/login` |
| `/navigation` | `pushHistoryStateIfNeeded`: removed error branch, merged push branches | `UI.navigate`: `/target`, Back returns to `/navigation`. Forward: `/target` (not `/forwarding`), one history entry added, Back returns to `/navigation`. Missing route: error view, no console errors |

`@Push(transport = WEBSOCKET)` is set on `Application`, because the default
`WEBSOCKET_XHR` skips the round trip. The background thread case is reached
only when the transport is not `WEBSOCKET`, so the view switches to long
polling when it gets the query parameter.

## Automated check

```
PLAYWRIGHT_CORE=<path to playwright-core> CHROME=<chrome binary> node check/check.cjs > result.json
check/csp-warnings.sh <app log file>
```

`check.cjs` drives all cases and prints the results as JSON.
`csp-warnings.sh` prints one line for each CSP warning: the API that sent the
string and the caller.

## Results (2026-11-05)

Baseline: `main` at 2b3f5291e9 (merge base of the PR). PR: 87c5b7cfd9.

The `check.cjs` output is the same for baseline and PR, with the React router
and with the Flow client router, browser console messages included. All cases
work.

`executeJs` warnings (the `addEventData` warnings are left out; they are not
part of this PR):

| Call site | Baseline | PR |
|---|---|---|
| `Element.executeJs` <- `Page.ensureResizeListener` | logged | — |
| `Element.executeJs` <- `Element.flashClass` | logged | — |
| `Page.executeJs` <- `AuthenticationContext.logout` (push) | logged | — |
| `Page.executeJs` <- `AuthenticationContext.logout` (background thread) | logged | — |
| `Page.executeJs` <- `Page.openInternal` <- `UidlRedirectStrategy` (logout redirect) | logged | logged (tracked in #26698) |

The removed `scrollPositionHandlerAfterServerNavigation` call cannot be
reached with the current client, not even on the baseline. With the Flow
client router, a click on a router link sends `trigger: "link"`, so the
trigger is `ROUTER_LINK`, but `historyState` is `null`. `event.getState()` is
then empty, and nothing was sent. With the React router the trigger is never
`link`. So removing it changes nothing for the user.
