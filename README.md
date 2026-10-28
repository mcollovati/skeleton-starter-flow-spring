# Reproducer: `addEventData(Class)`/`addEventDetail(Class)` do not work with `getEventData(Class)`/`getEventDetail(Class)`

`DomListenerRegistration.addEventData(Class)` and `DomEvent.getEventData(Class)` are meant to be used as a pair. But the client sends event data with flat keys, one key per expression:

```json
{"event.button": 0, "event.clientX": 68, "event.clientY": 159}
```

## Fixed by vaadin/flow#26716

Before vaadin/flow#26716, `getEventData(Class)` gave this JSON to Jackson without changes, and `getEventDetail(Class)` rebuilt the detail object only one level deep. So a nested record, as in the Javadoc of `addEventData(Class)`, was decoded with `null` nested properties (cases 1 and 5). vaadin/flow#26716 rebuilds nested objects by splitting the flat keys on dots. It is merged on `main` and picked to 25.3. It is not in Flow 25.3.5.

## Still open

- `addEventData(Class)` and `addEventDetail(Class)` use the Java property name, not the `@JsonProperty` name:
  - a flat record with `@JsonProperty("event.button")` registers the expression `button`, which throws `ReferenceError: button is not defined` in the browser, and the event is not sent at all (case 2);
  - for `@JsonProperty("r") int red` in a detail record, `addEventDetail(Class)` registers `event.detail.red`, which is `undefined` in the browser, so `red` is always `0` (case 6).
- A `@JsonProperty` name with dots works only on a top-level property. On a nested property, the key `event.detail.x` is split on every dot, but Jackson expects `{"event": {"detail.x": ...}}`, so the value is `0` (case 7).

## Running

Run `mvn spring-boot:run`, open http://localhost:8080 and click each "Click here" area. Cases 4 to 7 fire a custom event with a fixed `detail` object when clicked.

| Case | Setup | Flow 25.3.5 (Vaadin 25.3.4) | Flow `main` with #26716 |
|---|---|---|---|
| 1 | Nested record (Javadoc example), `addEventData(Class)` | `MouseEventData[event=null]` | `MouseEventData[event=EventDetails[button=0, clientX=68, clientY=159]]` (works) |
| 2 | Flat record with `@JsonProperty`, `addEventData(Class)` | `ReferenceError` in the browser console, nothing shown | same |
| 3 | Same flat record, expressions added as strings | `FlatMouseEventData[button=0, clientX=68]` (works) | same |
| 4 | Flat detail record, `addEventDetail(Class)` | `RgbColor[r=255, g=128, b=64]` (works) | same |
| 5 | Nested detail record, `addEventDetail(Class)` | `DragDetail[start=null, end=null]` | `DragDetail[start=Position[x=1, y=2], end=Position[x=3, y=4]]` (works) |
| 6 | Detail record with `@JsonProperty("r") int red`, `addEventDetail(Class)` | `RenamedRgbColor[red=0, g=128, b=64]` | same |
| 7 | Nested record with `@JsonProperty("detail.x")`, expression added as a string | `NestedDottedData[event=null]` | `NestedDottedData[event=Inner[x=0]]` |

To run against Flow `main`, install a Flow snapshot that includes vaadin/flow#26716 (`flow-server` and `flow-client`) and run `mvn spring-boot:run -Dvaadin.version=25.4-SNAPSHOT`.
