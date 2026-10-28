# Reproducer: `addEventData(Class)`/`addEventDetail(Class)` do not work with `getEventData(Class)`/`getEventDetail(Class)`

`DomListenerRegistration.addEventData(Class)` and `DomEvent.getEventData(Class)` are meant to be used as a pair. But the client sends event data with flat keys, one key per expression:

```json
{"event.button": 0, "event.clientX": 68, "event.clientY": 159}
```

`getEventData(Class)` gives this JSON to Jackson without changes, so:

- a nested record, as in the Javadoc of `addEventData(Class)`, is decoded with `event` set to `null`;
- a flat record with `@JsonProperty("event.button")` can be decoded, but `addEventData(Class)` then registers the expression `button`, which throws `ReferenceError: button is not defined` in the browser, and the event is not sent at all.

`addEventDetail(Class)` and `getEventDetail(Class)` have the same problems for `event.detail`:

- `getEventDetail(Class)` rebuilds the detail object only one level deep. For a nested record, the key `event.detail.start.x` becomes `start.x`, so `start` is decoded as `null`;
- `addEventDetail(Class)` uses the Java property name, not the `@JsonProperty` name. For `@JsonProperty("r") int red` it registers `event.detail.red`, which is `undefined` in the browser, so `red` is always `0`.

## Running

Run `mvn spring-boot:run`, open http://localhost:8080 and click each "Click here" area. Cases 4 to 6 fire a custom event with a fixed `detail` object when clicked.

| Case | Setup | Result |
|---|---|---|
| 1 | Nested record (Javadoc example), `addEventData(Class)` | `MouseEventData[event=null]` |
| 2 | Flat record with `@JsonProperty`, `addEventData(Class)` | `ReferenceError` in the browser console, nothing shown |
| 3 | Same flat record, expressions added as strings | `FlatMouseEventData[button=0, clientX=68]` (works) |
| 4 | Flat detail record, `addEventDetail(Class)` | `RgbColor[r=255, g=128, b=64]` (works) |
| 5 | Nested detail record, `addEventDetail(Class)` | `DragDetail[start=null, end=null]` |
| 6 | Detail record with `@JsonProperty("r") int red`, `addEventDetail(Class)` | `RenamedRgbColor[red=0, g=128, b=64]` |

Cases 1 to 3 were tested with Vaadin 25.3.4 and 25.2.8. Cases 4 to 6 were tested with Vaadin 25.3.4.
