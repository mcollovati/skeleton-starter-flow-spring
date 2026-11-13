# Native image: proxy of a JsSnippet sub-interface is not registered

Since vaadin/flow#26960, an application or a component can declare its own
snippet type as an interface that extends `JsSnippet`. Flow implements it with
a JDK proxy (`JsDefinitionProxy.createDeclaredSnippet`).

A native image can only create a JDK proxy that is registered at build time.
Both native integrations register proxies only for `@JsDefinition` interfaces:

- Quarkus: `VaadinQuarkusNativeProcessor.registerJsDefinitionProxies`
- Spring: `VaadinBeanFactoryInitializationAotProcessor.registerJsDefinition`

So the snippet type `Greeting` gets no proxy, and calling a definition method
that returns it fails in a native image.

## The view

`MainView` has two rows. Each row has a button that runs the same JavaScript:

| Row | Snippet type | Purpose |
|---|---|---|
| built-in | `JsFunction` (Flow's own) | control |
| custom | `Greeting extends JsSnippet` (the application's own) | the bug |

After a click, the row shows the server-side status (`OK` or `FAILED <error>`)
and the text that the client rendered. The first line of the page says whether
the server runs on the JVM or as a native image.

Expected on the JVM and in a native image, for both rows:

```
Status: OK
Hello, native image!
```

## Requirements

- Flow `25.4-SNAPSHOT` with vaadin/flow#26960 (main after 2026-11-13). The
  project uses only Flow and the Flow BOM, no platform components.
- GraalVM or Mandrel for JDK 25 as `JAVA_HOME` and `GRAALVM_HOME`, for the
  native builds.

## Spring Boot (this folder)

JVM:

```
mvn package
java -jar target/spring-skeleton-1.0-SNAPSHOT.jar
```

Native image:

```
mvn -Pnative native:compile
./target/spring-skeleton
```

Open http://localhost:8080/ and click both buttons.

## Quarkus (folder `quarkus`)

The same view classes, compiled from `../src/main/java` without the Spring
Boot entry point, with the Vaadin Quarkus extension.

JVM:

```
cd quarkus
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

Native image:

```
cd quarkus
mvn package -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false
./target/quarkus-native-snippet-type-1.0-SNAPSHOT-runner
```

Open http://localhost:8080/ and click both buttons.

## Result

Checked on 2026-11-13 with Flow main at 6373400bca, Mandrel 25.0.2, Spring
Boot 4.1.2 and Quarkus 3.33.4.2.

| | built-in row | custom row |
|---|---|---|
| Spring, JVM | OK | OK |
| Spring, native image | OK | **FAILED** |
| Quarkus, JVM | OK | OK |
| Quarkus, native image | OK | **FAILED** |

The same error in both native images:

```
org.graalvm.nativeimage.MissingReflectionRegistrationError: Cannot reflectively
access the proxy class inheriting ['org.vaadin.example.Greeting'].
	at java.lang.reflect.Proxy.newProxyInstance(Proxy.java:924)
	at com.vaadin.flow.js.JsDefinitionProxy.createDeclaredSnippet(JsDefinitionProxy.java:508)
	at com.vaadin.flow.js.JsDefinitionProxy.lambda$snippetFactory$4(JsDefinitionProxy.java:480)
	at com.vaadin.flow.js.JsDefinitionProxy$JsSnippetHandler.invoke(JsDefinitionProxy.java:685)
	at org.vaadin.example.MainView.lambda$new$1(MainView.java:34)
```

The runtime hints that Spring AOT writes to
`target/spring-aot/main/resources/META-INF/native-image/com.example/spring-skeleton/reachability-metadata.json`
have a `proxy` entry for `GreetingJs` and `RenderJs`, but not for `Greeting`.

Registering the proxy is enough. With this file in
`src/main/resources/META-INF/native-image/com.example/fix-check/reachability-metadata.json`,
the custom row shows OK in the Quarkus native image, with no errors in the
server log:

```json
{ "reflection": [ { "type": { "proxy": [ "org.vaadin.example.Greeting" ] } } ] }
```

## Notes for building

- Spring with an older GraalVM, such as Mandrel 25.0.2: the reachability
  metadata repository fails the build with "provides a reachability-metadata
  schema, but your GraalVM installation ... does not". Add
  `-Dnative.metadataRepository.enabled=false`.
- Quarkus: `quarkus/pom.xml` adds `jackson-core` (Jackson 2). Without it the
  native build fails in analysis, because Quarkus initializes
  `io.vertx.core.json.Json` at build time and its codec needs Jackson 2.
