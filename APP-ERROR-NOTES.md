# Clients stay active after an internal error

After the server answers a request with the "Internal error" notification
(`meta.appError`), the Flow clients keep reporting `isActive() == true`.
TestBench's `waitForVaadin()` checks `isActive()` on every client, so it then
waits for its full 40 s limit before every command. It returns without an
error, and the test only fails much later with an unclear timeout.

Found in flow-components ITs, where a `CspViolationException` escaped a
request. This reproducer uses any exception that escapes request handling,
because that alone causes the problem.

Version: Vaadin 25.4.0-alpha6 (no snapshot needed).

## How the reproducer causes the error

`FailNextUidlRequest` registers a `RequestHandler` that throws an exception for
the next UIDL request of the session after `arm()` is called. Flow's
`VaadinService.handleExceptionDuringRequest` then sends the "Internal error"
notification. (An exception in a component listener is not enough: Flow sends
a normal response for it.)

## Manual steps

1. Run `mvn` and open http://localhost:8080/app-error.
2. The box in the bottom right corner shows `isActive()` for each client.
3. ROOT client case: click "Fail the next request", then "Send a request".
   - "Internal error" is shown, and both clients are inactive.
   - About 5.5 s later, `ROOT: isActive() = true`, and it stays true.
   - In the browser's Network tab, the client sends the same request again
     every ~5.5 s.
4. TypeScript client case: reload the page, click "Fail the next request",
   then "Navigate to the target view".
   - "Internal error" is shown, and `TypeScript: isActive() = true` right away
     and stays true. ROOT becomes active about 5 s later, as in step 3.

Expected: all clients become inactive after the error.

## Integration tests

`mvn -Pit verify` runs `AppErrorIT` (TestBench, headless Chrome). Each test
checks the expected behavior, so **all four tests fail while the bug is
present**. Result on 25.4.0-alpha6:

| Test | Result |
|------|--------|
| `rootClientInactiveAfterInternalError` | ROOT becomes active again 5.6 s after the error and stays active |
| `typeScriptClientInactiveAfterFailedNavigation` | TypeScript active from the start, ROOT active from 5.1 s |
| `waitForVaadinReturnsAfterInternalError` | one TestBench command takes 40004 ms |
| `waitForVaadinReturnsAfterFailedNavigation` | one TestBench command takes 40005 ms |

## Causes (Flow client, main branch)

- **ROOT client** (`flow-client/.../communication/MessageSender.ts`,
  `MessageHandler.ts`): the error response has no `clientId`, so the resend
  timer started in `#scheduleResend` is never cleared. The UI lifecycle
  handler in `#listenToUILifecycle` does not call `#resetTimer()` when the UI
  terminates, and the timer callback does not check for termination. The
  timer sends the request again and calls `startRequest()`.
  `handleMessage` then drops the reply because the UI is terminated, but does
  not end the request, so `hasActiveRequest()` stays true. The GWT client
  before the TypeScript port had the same code, so this is not a regression.
- **TypeScript client** (`flow-client/src/main/frontend/Flow.ts`):
  `flowNavigate()` calls `loadingStarted()`, and only the `serverConnected` or
  `serverPaused` callback calls `loadingFinished()`. The error response calls
  neither, so `isActive` stays true and the navigation promise never ends.
