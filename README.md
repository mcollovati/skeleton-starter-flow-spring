# Reproducer: `addEventData(Class)` and `getEventData(Class)` do not work together

`DomListenerRegistration.addEventData(Class)` and `DomEvent.getEventData(Class)` are meant to be used as a pair. But the client sends event data with flat keys, one key per expression:

```json
{"event.button": 0, "event.clientX": 68, "event.clientY": 159}
```

`getEventData(Class)` gives this JSON to Jackson without changes, so:

- a nested record, as in the Javadoc of `addEventData(Class)`, is decoded with `event` set to `null`;
- a flat record with `@JsonProperty("event.button")` can be decoded, but `addEventData(Class)` then registers the expression `button`, which throws `ReferenceError: button is not defined` in the browser, and the event is not sent at all.

## Running

Run `mvn spring-boot:run`, open http://localhost:8080 and click each "Click here" area.

| Case | Setup | Result |
|---|---|---|
| 1 | Nested record (Javadoc example), `addEventData(Class)` | `MouseEventData[event=null]` |
| 2 | Flat record with `@JsonProperty`, `addEventData(Class)` | `ReferenceError` in the browser console, nothing shown |
| 3 | Same flat record, expressions added as strings | `FlatMouseEventData[button=0, clientX=68]` (works) |

Tested with Vaadin 25.3.4 and 25.2.8.
