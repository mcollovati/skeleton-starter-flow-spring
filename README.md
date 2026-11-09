# Typed JavaScript results (vaadin/flow#25811, PR #26890)

This branch tests typed results of `@JsDefinition` methods
(`JavaScriptResult<T>`), from PR https://github.com/vaadin/flow/pull/26890.

## Setup

The app uses `25.4-SNAPSHOT`. Install Flow from the PR branch
`25811-typed-js-result-prototype` first. In the Flow repository:

```
git checkout 25811-typed-js-result-prototype
mvn install -DskipTests -Dcheckstyle.skip=true -pl flow-server,flow-build-tools,flow-plugins/flow-plugin-base,flow-plugins/flow-maven-plugin,flow-client -am
```

Then run this app with `-nsu`, so that Maven does not take a newer remote
snapshot instead of the local one:

```
mvn -nsu spring-boot:run
```

## What to check

Open http://localhost:8080/ and click "Run all". Every row shows the
expected result and the actual one. They must be the same.

- **Conversion**: `Integer`, a `Person` record, `List<Person>`, and a
  `Promise` read through `toCompletableFuture()`.
- **JavaScript null**: the `then` handlers of `JavaScriptResult<JsonNode>` get
  a `NullNode`, the same as `then` of `PendingJavaScriptResult`. `ObjectNode`
  and `Integer` get a Java `null`. `toCompletableFuture()` completes with a
  Java `null` for every type, also `JsonNode` (the view does not test this).
- **Failures**: a value that can not be converted goes to the error handler.
  Without an error handler it is logged as a WARN by `JsDefinitionProxy`, and
  a `toCompletableFuture()` added after it on the same result still fails.
  A wrong JSON node type (an array for `ObjectNode`) goes to the error handler.
- **Inherited**: `IntValueJs extends ValueJs<Integer>` and
  `PeopleValueJs extends ValueJs<List<Person>>` convert to the bound type.
  `RawValueJs extends ValueJs` (raw) is refused. At startup, the build logs
  an ERROR "Leaving the JavaScript of ...RawValueJs out of the bundle". This
  is expected.

Then click "Run PendingJavaScriptResult case". This is the released API,
for comparison: `PendingJavaScriptResult.toCompletableFuture(Integer.class)`
with the value `'abc'`. The future should fail, but today it never completes:
the row stays at "future still pending", and the server log shows an
`IllegalArgumentException` from `DefaultErrorHandler`. This is a bug in the
released code, not in the PR.

# Project Base for Vaadin and Spring Boot

This project can be used as a starting point to create your own Vaadin application with Spring Boot.
It contains all the necessary configuration and some placeholder files to get you started.

The best way to create your own project based on this starter is [start.vaadin.com](https://start.vaadin.com/) - you can get only the necessary parts and choose the package naming you want to use.

## Running the Application
There are two ways to run the application :  using `mvn spring-boot:run` or by running the `Application` class directly from your IDE.

You can use any IDE of your preference,but we suggest Eclipse or Intellij IDEA.
Below are the configuration details to start the project using a `spring-boot:run` command. Both Eclipse and Intellij IDEA are covered.

#### Eclipse
- Right click on a project folder and select `Run As` --> `Maven build..` . After that a configuration window is opened.
- In the window set the value of the **Goals** field to `spring-boot:run` 
- You can optionally select `Skip tests` checkbox
- All the other settings can be left to default

Once configurations are set clicking `Run` will start the application

#### Intellij IDEA
- On the right side of the window, select Maven --> Plugins--> `spring-boot` --> `spring-boot:run` goal
- Optionally, you can disable tests by clicking on a `Skip Tests mode` blue button.

Clicking on the green run button will start the application.

After the application has started, you can view your it at http://localhost:8080/ in your browser.


If you want to run the application locally in the production mode, use `package` and `java -jar target/spring-skeleton-1.0-SNAPSHOT.jar` commands instead.
### Running Integration Tests

Integration tests are implemented using [Vaadin TestBench](https://vaadin.com/testbench). The tests take a few minutes to run and are therefore included in a separate Maven profile. We recommend running tests with a production build to minimize the chance of development time toolchains affecting test stability. To run the tests using Google Chrome, execute

`mvn verify -Pit`

and make sure you have a valid TestBench license installed.

Profile `it` adds the following parameters to run integration tests:
```sh
-Dwebdriver.chrome.driver=path_to_driver
-Dcom.vaadin.testbench.Parameters.runLocally=chrome
```

If you would like to run a separate test make sure you have added these parameters to VM Options of JUnit run configuration

### Live Reload (optional)

With live reload, you can see the results of your code changes immediately. 
When you edit your Java code and recompile it, the application changes will be automatically reloaded and the browser is refreshed.
This is done by leveraging [Spring Boot Developer Tools](https://docs.spring.io/spring-boot/docs/2.1.5.RELEASE/reference/html/using-boot-devtools.html). 
To be able to see the changes in the browser tab, the page still needs to be reloaded. 
That can also  be automated via a LiveReload browser extension. 
One such extension for Google Chrome is [LiveReload](https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei). 
In Firefox, [LiveReload - Web extension](https://addons.mozilla.org/en-US/firefox/addon/livereload-web-extension/) can be used.
You can find such similar extensions for other major browsers too.
These extensions add an icon to your browser next to the address bar.
To enable the extension, you should click that icon after you opened your application. 

You can find more information at [Live Reload in Spring Boot Applications](https://vaadin.com/docs/flow/workflow/tutorial-spring-boot-live-reload.html) document.

## Structure

Vaadin web applications are full-stack and include both client-side and server-side code in the same project.

| Directory                                  | Description |
|:-------------------------------------------| :--- |
| `src/main/frontend/`                       | Client-side source directory |
| &nbsp;&nbsp;&nbsp;&nbsp;`index.html`       | HTML template |
| &nbsp;&nbsp;&nbsp;&nbsp;`index.ts`         | Frontend entrypoint |
| &nbsp;&nbsp;&nbsp;&nbsp;`main-layout.ts`   | Main layout Web Component (optional) |
| &nbsp;&nbsp;&nbsp;&nbsp;`views/`           | UI views Web Components (TypeScript / HTML) |
| &nbsp;&nbsp;&nbsp;&nbsp;`styles/`          | Styles directory (CSS) |
| `src/main/java/<groupId>/`                 | Server-side source directory |
| &nbsp;&nbsp;&nbsp;&nbsp;`Application.java` | Server entrypoint |
| &nbsp;&nbsp;&nbsp;&nbsp;`AppShell.java`    | application-shell configuration |

## Code Formatting

The project includes the Spotless code formatter.

To use it in IntelliJ, install the [https://plugins.jetbrains.com/plugin/22455-spotless-applier](IntelliJ plugin)
To use it in VS Code, install the [https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle ](VS Code extension)
To use it from the command line, run `mvn spotless:apply`

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified at [vaadin.com/learn/training](https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/components](https://vaadin.com/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Discover Vaadin's set of CSS utility classes that enable building any UI without custom CSS in the [docs](https://vaadin.com/docs/latest/ds/foundation/utility-classes). 
- Find a collection of solutions to common use cases in [Vaadin Cookbook](https://cookbook.vaadin.com/).
- Find Add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin/platform).
