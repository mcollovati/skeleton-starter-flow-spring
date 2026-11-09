# CSP feedback from browserless tests

Question: if a browserless test runs with `vaadin.csp=warn` or
`vaadin.csp=strict`, does it tell early what an application must change to
work under a strict Content Security Policy?

Based on `v25.4` of the skeleton (Vaadin 25.4.0-alpha5, the first version with
the `csp` setting and the string JavaScript reporter).

## Short answer

- **warn: yes, with a filter.** Every server-side call that sends JavaScript
  as a string is logged with a stack trace, also in browserless tests. But most
  warnings come from Flow itself (`UI`, `Page`) and from Vaadin components
  (`Button`, click shortcuts), not from the application. The test helper
  `CspViolationCollector` sorts them into APP and VAADIN.
- **strict: no, not today.** `new UI()` already throws
  `CspViolationException` (the `Page` constructor adds a resize listener with
  event data), so every browserless test fails in its setup, before the test
  code runs.

## What is in the branch

- `CspView` (`/csp`): three UNSAFE call sites (`executeJs(String)` in
  `onAttach` and in a click listener, `addEventData` and `setFilter` on a DOM
  listener) and one SAFE call (`executeJs(TitleJs.class)`, a `@JsDefinition`).
- `CspViolationCollector`: JUnit extension. It listens to the
  `CspViolationReporter` logger and gives each warning an owner:
  - `APP`: application code called the API directly. The application must
    change.
  - `VAADIN`: Flow or a component made the call. The report shows the
    application line that triggered it, if there is one.

  It prints a report after each test class. With
  `-Dcsp.failOnAppViolations=true`, a test fails when it reaches an APP call
  site.
- `CspViewCspTest`, `MainViewCspTest`: tests with
  `@SpringBootTest(properties = "vaadin.csp=warn")`.

## Steps

1. `./mvnw test`

   All tests pass. Look for `===== CSP report for CspViewCspTest =====` in the
   output:

   ```
   APP  ...addEventData(ElementListenerMap.java:175)  <-  CspView.<init>(CspView.java:30)
   APP  ...setFilter(ElementListenerMap.java:236)  <-  CspView.<init>(CspView.java:31)
   APP  Element.executeJs(Element.java:1957)  <-  CspView.onAttach(CspView.java:39)
   APP  Page.executeJs(Page.java:601)  <-  CspView.lambda$new$0(CspView.java:21)
         reached by: [clickingReportsTheListenerCode()]
   VAADIN  Page.<init>(Page.java:91)  <-  UI.<init>(UI.java:203) (...)
   VAADIN  ...addEventData(...)  <-  ComponentEventBus.addDomTrigger(...) (triggered from MainView.<init>(MainView.java:41))
   ...
   ```

   The SAFE `executeJs(TitleJs.class)` call (`CspView.java:42`) is not in the
   report. `MainViewCspTest` has only VAADIN entries: the skeleton view itself
   is fine, but `Button` and `addClickShortcut` are not.

2. `./mvnw test -Dtest=CspViewCspTest -Dcsp.failOnAppViolations=true`

   Both tests fail and list the APP call sites. This is how a project could
   gate on its own code in CI.

3. `./mvnw test -Dtest=MainViewTest -Dvaadin.csp=strict`

   All 5 tests fail with `CspViolationException` from
   `Page.<init>` &larr; `UI.<init>` &larr; `MockVaadin.setup`.

## Limits of this feedback

- It only sees code that the tests run. The click listener line is found only
  by the test that clicks the button.
- Calls without an application frame (deferred work in
  `beforeClientResponse`, for example `ShortcutRegistration`) cannot be tied
  to an application line.
- It only sees the server-side string JavaScript APIs. It does not see what
  happens in the browser: `eval` or `new Function` in npm packages or in your
  own frontend code, inline `<script>` or event handler attributes in
  templates, inline styles, the policy header itself. Those need a browser
  test (TestBench) against a production build with `vaadin.csp=strict`.
- Flow logs each call site once per `VaadinService`. Browserless tests create
  a new service for each test, so this does not hide warnings between tests.
- Spring Boot resets logback when it starts the test context, so the appender
  is attached again before each test.
