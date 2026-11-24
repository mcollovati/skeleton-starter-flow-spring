# Reproducer: pre-built bundles that carry more JavaScript definitions than the application

Checks [vaadin/flow#27076](https://github.com/vaadin/flow/pull/27076): the bundle
check accepts a pre-built bundle that registers every JavaScript function the
application needs, even when the bundle registers more. Before, the check
compared the whole generated `vaadin-js-definitions.js` file, so a bundle built
with more definitions than the application has, such as the pre-built
production and development bundles, was always rebuilt.

The application is the skeleton with `vaadin-core` instead of `vaadin`, so it
has fewer definitions than the pre-built bundles, which are built with all
components. Profiles add the other cases.

| Profile | What the application has |
| --- | --- |
| (none) | the free components only |
| `all-components` | all components, as the pre-built bundles |
| `app-definition` | a view with a `@JsDefinition` of its own (`src/app-definition`) |
| `shared-function` | a view with a `@JsDefinition` that declares the same JavaScript as `IFrame.ReloadJs` (`src/shared-function`), so both are one function in the bundle, under the name of `IFrame` |

## Setup

The pre-built bundles must be built with the change:

1. Apply vaadin/flow#27076 on Flow `25.4` and install it.
2. Build vaadin/platform with Flow `25.4-SNAPSHOT`:
   `scripts/generateBoms.sh --useSnapshots`, then
   `mvn install -Doss -pl :vaadin-prod-bundle-unoptimized,:vaadin-prod-bundle,:vaadin-dev-bundle -am`.
   The `stats.json` of each bundle lists `jsDefinitionFunctionIds`, and the
   one of the development bundle also lists `jsDefinitionNames`.

Before each run, delete `src/main/bundles/`. A run that compiles an
application bundle saves it there, and the next run is then checked against
that bundle instead of the pre-built one.

## Steps

1. Production: `mvn clean package -DskipTests [-P<profile>]`
2. Development: `mvn compile spring-boot:run [-P<profile>]`, and read the log
   lines of `BundleValidationUtil`.

## Results

Flow `25.4` (59895a8a41) with vaadin/flow#27076 at c7199239ba, flow-components
`25.4` (bc50b09), bundles from vaadin/platform main (4d7eb1ed).

| Profile | Production | Development |
| --- | --- | --- |
| (none) | `A production mode bundle build is not needed` | `A development mode bundle build is not needed` |
| `all-components` | not needed | not needed |
| `app-definition` | `Detected JavaScript that the bundle does not carry, declared by: org.vaadin.example.appdefinition.AppDefinitionView$MarkJs`, then a build | the same message, then a build |
| `shared-function` | not needed | not needed |

Without the change, the check compared the hash of the whole generated file.
That hash differs from the one the bundle recorded for `shared-function` in
production (155 functions the application needs, 181 in the bundle) and for
the free components in development (159 named functions, 182 in the bundle),
so these runs would build a bundle.
