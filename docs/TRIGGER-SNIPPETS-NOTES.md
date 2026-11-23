# Trigger, paste and initializer snippets under `vaadin.csp=strict`

Cases for vaadin/flow#26889 (fixes vaadin/flow#26668). The application runs
in production mode with `vaadin.csp=strict`, so Vaadin sends a
Content-Security-Policy without `unsafe-eval`, and the browser refuses any
JavaScript compiled from a string. Every case checks that the feature works
and that the page reports no CSP violation and no console error.

Views:

- `/trigger-snippets` (`TriggerSnippetsView`): the trigger API and the
  feature facades built on it
- `/paste-snippets` (`PasteSnippetsView`): `Clipboard.onPaste` and
  `Clipboard.onFilePaste`
- `/initializer-snippets` (`InitializerSnippetsView`): the new
  `Element.addJsInitializer(JsFunction)` and the forms that are refused

## What it needs

Flow from the PR branch `csp-trigger-snippets`, installed as 25.4-SNAPSHOT:

```
git fetch origin pull/26889/head:pr-26889
git switch --detach pr-26889
mvn install -DskipTests
```

The other 25.4-SNAPSHOT artifacts (platform BOM, components) must already be
in the local repository. Build offline (`-o`), so Maven does not replace the
local Flow snapshots with remote ones. The ITs use the installed Google Chrome
through Playwright, so no browser is downloaded.

```
mvn -o -Pit verify
```

To use another port, add
`-Dspring-boot.run.arguments=--server.port=<port> -Dit.baseUrl=http://localhost:<port>`.

## Cases

| Case | What it checks | Expected |
|------|----------------|----------|
| CSP | the policy header of the page | present, without `unsafe-eval` |
| W1 | `Clipboard.onClick(button).writeText(literal, onCopied, onError)` with quotes in the text | `copied=<text>` |
| R1 | `Clipboard.onClick(button).readText(...)` after W1 | `read=<text>` |
| W2 | `Clipboard.onClick(button).writeText(input, ...)`, the value is read in the browser | `copied=typed value`, and the clipboard has it |
| D1 | `Download.onClick(button, url, fileName)` with apostrophes in the name | a download with that name and the file content |
| D2 | `Download.onClick(button, DownloadHandler)` | a download with the handler's name and content |
| N1 | `Download.onClick(button, url, DownloadTarget.NEW_TAB)` (was `OpenInNewTab.onClick`, removed by #27074) | a popup with the URL |
| N2 | `OpenInNewTabAction` with a `javascript:` URL read from an input | no popup |
| F1 | `Fullscreen.onClick(button).enter(component, ...)` | `entered`, and the fullscreen element contains the component |
| S1 | `WebShare.onClick(button).share(title + url, ...)` | `shared`, and `navigator.share` got `{title, url}` |
| Z1 | `SizeTrigger` with two `SetPropertyAction`s on a resizable panel | the size of the panel, and the new width after a resize |
| T1 | `UI.triggerAfter(300 ms, ...)` (timeout installer) | `fired` |
| E1 | `DomEventTrigger` on `input` with `SetSignalAction` from a `PropertyInput` | the typed text |
| E2 | `DoubleClickTrigger` with a client-only `SetPropertyAction` | `double` |
| P1 | `Clipboard.onPaste(target, ...)` with text and HTML | `text=plain;html=<b>bold</b>` |
| P2 | `Clipboard.onPaste(view, PasteOptions.defaults(), ...)`: a paste in an input, then in a div | only the div paste is reported |
| P3 | `Clipboard.onFilePaste(target, ...)` with a pasted file (upload installer through `addJsInitializer(JsFunction)`) | `files=1;last=pasted.txt` |
| I1 | `addJsInitializer(JsFunction)` with a declared initializer that returns a cleanup: register, detach, reattach, remove | init 1, cleanup 1, init 2, cleanup 2 |
| I2 | `addJsInitializer(JsFunction)` with a function that declares a runtime argument (`@JsFunctionParameterNames`) | `IllegalArgumentException` |
| I3 | `addJsInitializer(String)` | `CspViolationException` |
| I4 | `addJsInitializer((JsFunction) null)` | `NullPointerException` |
| I5 | `JsFunction.of(String)` | `CspViolationException` |

## Testing by hand

Each view says how to test every case and what to expect, under the case
title. Keep the browser console open: no case may log a CSP violation or an
error.

## What the cases cannot show

- **Share (S1):** Chrome on Linux has no `navigator.share`. The IT installs a
  stub before the page loads, outside the page's policy, that records the data
  it gets. So S1 checks that the declared snippet runs and passes the right
  data, not that the browser shares.
- **Paste (P1-P3):** the IT dispatches a `paste` event with a `DataTransfer`
  from Playwright, which is not subject to the page's policy. The Flow side
  (declared event data, filter and upload installer) is the same as for a real
  paste.
- **`withArguments`:** an initializer created with `JsFunction.withArguments`
  cannot be built under strict CSP (`JsFunction.of` is refused, and
  `withArguments` on a declared function throws), so only the
  `@JsFunctionParameterNames` refusal (I2) is checked.
- **File names:** Chrome replaces `"` in a download name with `_`, so D1 uses
  apostrophes.

## Results

All 18 ITs pass on the PR head c303cd4099 (2026-11-16).
