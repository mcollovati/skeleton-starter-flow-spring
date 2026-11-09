# Reproducer: resynchronization request over WebSocket push is never answered

With push transport `WEBSOCKET`, the client sends its requests over the
WebSocket too. When a pushed message is lost, the client asks the server to
resynchronize. On the WebSocket path the server does not answer: `PushHandler`
does not handle `ResynchronizationRequiredException`, so it is logged as
"Unexpected error" and the UI stops updating. The client still reports itself
as `connected`.

The XHR path (`UidlRequestHandler`) handles the same exception and sends the
full state, so with `WEBSOCKET_XHR` the same lost message is recovered.

## The app

- `PushCounterView` (`/`) pushes a counter every 500 ms, with no user input.
- `Application` uses `@Push(transport = Transport.WEBSOCKET)` and inlines
  `src/main/resources/drop-one-push.js` into the page. That script drops one
  message the server pushes (the 6th), as a proxy restart or a network problem
  does.

## Steps

1. `mvn spring-boot:run` (or `mvn package` and `java -jar target/spring-skeleton-1.0-SNAPSHOT.jar`).
2. Open http://localhost:8080/ and watch the counter.

| URL | Result |
|---|---|
| `/` | The counter stops at 5, about 5 s after the dropped message. `window.Vaadin.connectionState.state` is still `connected`. |
| `/?nodrop` | No message is dropped: the counter keeps going. |
| `/?transport=WEBSOCKET_XHR` | Same dropped message: the client resynchronizes over HTTP and the counter keeps going. |

Server log for `/` (line numbers from 25.4.0-alpha5):

```
WARN  c.v.f.s.communication.ServerRpcHandler : Resynchronizing UI by client's request. A network message was lost before reaching the client and the client is reloading the full UI state. ...
ERROR c.v.flow.server.DefaultErrorHandler     : Unexpected error: null
com.vaadin.flow.server.communication.ServerRpcHandler$ResynchronizationRequiredException
	at com.vaadin.flow.server.communication.ServerRpcHandler.handleRpc(ServerRpcHandler.java:513)
	at com.vaadin.flow.server.communication.ServerRpcHandler.handleRpc(ServerRpcHandler.java:383)
	at com.vaadin.flow.server.communication.PushHandler.lambda$new$1(PushHandler.java:209)
	at com.vaadin.flow.server.communication.PushHandler.callWithUi(PushHandler.java:370)
	...
```

Checked on Vaadin 25.3.5 (the version in `pom.xml`) and on 25.4.0-alpha5
(`-Dvaadin.version=25.4.0-alpha5`).


## Automated check

`PushResyncIT` opens the three URLs in Chromium with Playwright for Java and
checks that the counter keeps going. Run it with:

```
mvn verify -Pit
```

The `it` profile starts the app on port 8080 and adds Playwright as a test
dependency, so the normal build does not change. Playwright downloads its own
Chromium on the first run. While the bug is there, one test fails and the two
controls pass:

```
[ERROR] PushResyncIT.websocketRecoversFromALostMessage ... 'http://localhost:8080/': the pushed counter stopped (now 5). Connection state: connected
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0
```
