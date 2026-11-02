# Declared DOM event data and filters

The `/event-data` view (`EventDataView`) shows how `DomEventData` and
`DomEventFilter` work. They come from the Flow branch
`25658-event-data-expression-parameters`. Install that branch first (flow-server
and flow-client, see the note about `vaadin-flow-client`), then run:

```
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dvaadin.frontend.hotdeploy=true"
```

This branch starts from `59bec19` (before the `addDynamicImport` demo), because
the Flow branch does not have `addDynamicImport(JsFunction)`.

Every case on the page says what to do and what to expect. Results below were
checked with headless Chrome on 2026-11-02, Flow at `3d78f7a61e`.

## Part 1: use cases (all work)

| Case | Result |
|---|---|
| 1. String value | `hello from data-label (String)` |
| 2. Same method, two captures, one listener | `a = first, b = second` |
| 3. Typed values | Integer, Boolean, record `Point` and `List<String>` decode correctly |
| 4. Captured component as argument | x relative to another element: 7 near the left, 392 near the right |
| 5. Key filters, same method, two captures | only Enter and Escape are sent |
| 6. Filter with two captures | `s` and Ctrl+S are told apart |
| 7. Filter with debounce | one `TRAILING` event after typing stops |
| 8. Declared and string API together | both values are read; see finding 5 |

With `vaadin.csp=warn`, the declared snippets log no CSP warning. Only the
string API calls do, and also the click listeners of the helper buttons
(Flow's `ClickEvent` uses string event data).

## Part 2: code review findings

- **F1, captured element removed: confirmed.** After the captured element is
  removed, a click sends nothing, not even to the plain listener on the same
  element. Console: `Cannot read properties of null (reading 'getDomNode')`.
  It works again when the element is added back.
- **F2, capture created before attach: shown.** A capture created in the
  constructor is `null` in the browser, and one created in `onAttach` is not.
  The Javadoc now documents this (commit `3d78f7a61e`).
- **F3, merged debounce settings: confirmed, and worse than the review said.**
  The Enter filter (no debounce) and the Escape filter (300 ms trailing) both
  come from `isKey(...)`, so they share debounce settings. On Enter, the browser
  flushes the trailing debouncer at once and sends one event with
  `"for":"trailing"`. The Enter listener does not accept that phase, so **Enter
  is lost**. The same pair with string filters works (`LEADING` for Enter).
- **F4, event data function throws: not new.** A throwing declared snippet
  (a stand-in for a function missing from the bundle) stops the whole event.
  A throwing string expression does the same.
- **Finding 5, hashed keys: shown in case 8.** The raw event data has the
  string expression under its own name, but the declared value and the string
  filter result are under hashed keys. `getEventData().get(filterExpression)`
  returns `null`.
