# CSP reproducers, all in one application

This branch (`repro/csp-all`) collects the use cases of the CSP reproducer
branches into one Spring Boot application. The original branches are not
changed. Use them when you need the exact state that was tested for a PR.

Vaadin 25.4-SNAPSHOT (Flow `main`). The application runs with
`vaadin.csp=strict`. Open http://localhost:8080/ for an index of all cases.

## Where each case comes from

| Package / folder | Source branch | Flow PR or issue | Notes |
|---|---|---|---|
| `snippets` | `repro/js-snippets` | #26830 | `docs/CSP-NOTES.md`, `docs/DYNAMIC-IMPORT-NOTES.md` |
| `triggers` | `repro/trigger-snippets-26889` | #26889 | `docs/TRIGGER-SNIPPETS-NOTES.md` |
| `errors` | `repro/declared-js-errors-27025` | #27025 | `docs/DECLARED-JS-ERRORS-NOTES.md` |
| `executejs` | `repro/declare-simple-executejs-calls` | #26699 | `docs/PR-26699-NOTES.md` |
| `eventdata` | `repro/event-data-snippets` | #25658 | `docs/EVENT-DATA-NOTES.md` |
| `eventdatapath` | `repro/add-event-data-path` | #26999, #27001 | `docs/EVENT-DATA-PATH-NOTES.md` |
| `bundledeventdata` | `repro/bundled-event-data` | #27000 | `docs/BUNDLED-EVENT-DATA-NOTES.md` |
| `domfilters` | `repro/bundled-dom-event-filters-27023` | #27023 | `docs/DOM-EVENT-FILTER-NOTES.md` |
| `shortcuts` | `repro/declare-shortcut-javascript` | #26953 | `docs/SHORTCUT-NOTES.md` |
| `components` | `repro/grid-strict-csp`, `repro/charts-strict-csp` | | `docs/GRID-CSP-NOTES.md`, `docs/CHARTS-CSP-NOTES.md` |
| `browserless` | `repro/csp-browserless` | | `docs/CSP-BROWSERLESS.md` |
| `addon-fallback/` | `repro/addon-csp-fallback-ecosystem` + case 15 of `repro/addon-csp-fallback-layer1` | #26153, PR #27128 | `addon-fallback/README.md` |

Packages are under `org.vaadin.example`, in `src/main/java` and
`src/test/java`. Routes are the same as in the source branches.

The notes in `docs/` are copied from the source branches. Their results and
dates describe the Flow build that was tested at that time. Build commands in
them refer to the layout of the source branch.

## Changes compared with the source branches

- `/` is now the index of all cases. The skeleton view moved to `/skeleton`
  (`browserless.SkeletonView`). The index views of the shortcut and
  `executejs` branches are now `/shortcut-cases` and `/executejs-cases`.
- `event-data-snippets` had an older copy of `SnippetView` (from before
  vaadin/flow#26830). Only the `js-snippets` version is here.
- Spring Security comes from `declare-simple-executejs-calls`. Only the
  `executejs` views need a login (`user` / `user`). All other views are
  `@AnonymousAllowed`.
- That branch set `@Push(transport = WEBSOCKET)` on `Application`. Here only
  `LogoutView` turns on push, so that push does not change the other cases.
- `vaadin.csp=strict` for all cases. The `js-snippets`, `event-data-snippets`,
  `declare-simple-executejs-calls` and shortcut branches used `warn`. Use
  `-Dvaadin.csp=warn` to get the warnings instead.
- The Node check scripts of `declare-simple-executejs-calls` (`check/`) are
  not included.
- `@AnonymousAllowed` is written fully qualified on the `@Route` line, so
  that line numbers in the views stay the same as in the source branches
  (tests and notes refer to them).
- `/sample.txt` is public in `SecurityConfig`, for the trigger cases.
- The shortcut review tests run with `vaadin.csp=warn`, the default of their
  source branch.
- Updated to the merged Flow API: `@JsCalledWith` is now
  `@JsFunctionParameterNames` (vaadin/flow#27018), and `OpenInNewTab.onClick`
  is now `Download.onClick(..., DownloadTarget.NEW_TAB)` (vaadin/flow#27074),
  and every `@JsDefinition` extends `JsInvokers` or `JsSnippets`
  (vaadin/flow#27152).
- Not included: `csp-demo` (no own commits), `repro/event-data-bean-decoding`
  (25.3, not CSP), `repro/native-snippet-type-proxy` (Quarkus native) and
  `repro/js-definition-optimize-bundle` (a build-time bundle issue).

## Running

Install Flow `main` (including `flow-client`) first, see the notes.

```
./mvnw spring-boot:run                    # development mode
./mvnw test                               # browserless and review tests
# all Playwright ITs, production mode; -Dtest=NONE skips the unit tests,
# because some of them fail on purpose (see below)
./mvnw -Pit clean verify -Dvaadin.force.production.build=true \
    -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -Pit verify -Dit.test=GridCspIT    # one IT
```

The ITs use Playwright and open `http://localhost:8080` (change it with
`-Dit.baseUrl`).

## Results on 2026-11-23 (Flow `main` 99a61c1043)

Playwright ITs: 94 of 94 pass. Unit and browserless tests: 13 of 14 pass.

`DelegateOptionsTest` fails on purpose: it reproduces an open bug (see
`docs/SHORTCUT-NOTES.md`).

Four assertions were updated to Flow `main`, because Flow changed after the
source branch was tested (the notes in `docs/` say what changed):

- `BundledEventDataIT` A5 and `EventDataPathIT` E2: the expression is now
  bundled (vaadin/flow#27000), so strict mode accepts it.
- `CspViewCspTest`: `addEventData("event.key")` is a property path and is
  not compiled since vaadin/flow#26916.
- `SkeletonViewCspTest`: the view reaches no string JavaScript at all.

Not checked here: the login and logout cases of `executejs` (no automated
test), and `addon-fallback/` (needs a build of PR #27128).

### Add-on fallbacks

`addon-fallback/` is a separate Maven project (an add-on module and an
application module). It is not part of the root build, because it needs the
open Flow PR #27128. Build it with a local Flow build of that PR:

```
./mvnw -f addon-fallback/pom.xml -Pit verify
```

See `addon-fallback/README.md`.
