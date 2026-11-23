# Proving the JsSnippet path needs no `unsafe-eval`

The `/snippet` view is run under Vaadin's built-in CSP feature, set with
`vaadin.csp` in `application.properties` (init parameter `csp`, see
`com.vaadin.flow.server.CspMode`). Do **not** add a hand-written CSP servlet
filter — a raw `script-src 'self' 'unsafe-inline'` blocks Vaadin's own
nonce'd bootstrap scripts and nothing renders. Vaadin builds the correct
nonce-based policy itself in strict mode.

## `vaadin.csp=warn` (what this branch ships) — the usable proof

The app boots normally and no CSP header is sent; `warn` only logs, once per
call site, every API that would need `unsafe-eval`.

Verified in a browser: all seven cases on `/snippet` produce their expected
text, with no console errors.

Server-side proof (from the app log): exactly one CSP warning names this view —

```
com.vaadin.flow.internal.CspViolationReporter : JavaScript is sent to the
browser as a string, which ... a Content Security Policy without 'unsafe-eval'
blocks.
    ...
    at org.vaadin.example.SnippetView.<init>(SnippetView.java:120)
```

Line 120 is the `viaString` case — a string `executeJs(...)`. The six declared
snippet cases (`labeled`, `greeter`, `constant`, `join`, `upper`, `prefixed`)
produce **no** CSP warning, because they send only function ids and captures and
compile nothing. That is the proof: the declared path is eval-free; only the
string path is not.

## `vaadin.csp=strict` — correct header, but not yet bootable

Set `vaadin.csp=strict` and rebuild to see the real enforced policy. Vaadin
emits a proper nonce-based header, e.g.

```
Content-Security-Policy: default-src 'self';
  script-src 'nonce-...' 'strict-dynamic'; ... object-src 'none'; base-uri 'self'
```

No `unsafe-eval`, no `unsafe-inline` for scripts. But the app returns **500** at
bootstrap:

```
Caused by: com.vaadin.flow.server.CspViolationException: JavaScript is sent to
the browser as a string ...
    at com.vaadin.flow.internal.nodefeature.ElementListenerMap$DomEventListenerWrapper.addEventData(ElementListenerMap.java:175)
    at com.vaadin.flow.dom.DomListenerRegistration.addEventDetail(DomListenerRegistration.java:568)
    at com.vaadin.flow.component.page.Page.<init>(Page.java:90)
    at com.vaadin.flow.component.UI.<init>(UI.java:203)
```

This is **not** caused by the snippet feature. Vaadin's own `UI`/`Page`
constructor registers a DOM event listener whose event data is a string
expression (compiled with `new Function`), which strict mode refuses. It is the
documented limitation on `CspMode.STRICT` ("Vaadin itself still uses APIs that
need eval ... the mode is not yet usable for applications") and the reason the
broader effort (`DomEventData`/`DomEventFilter`, vaadin/flow#25658) exists.

So an end-to-end *browser* proof under a fully enforced strict CSP is not
possible yet in a real app — through no fault of the declared-snippet path. The
`warn`-mode result above is the current, honest demonstration; the `strict`
header above shows the policy the feature is built for.

## How to reproduce

```
# install the feat/js-snippets Flow snapshot first (see the branch README notes)
mvn -Pproduction package -DskipTests
java -jar target/spring-skeleton-1.0-SNAPSHOT.jar
# open http://localhost:8080/snippet ; check the app log for CSP warnings
```
