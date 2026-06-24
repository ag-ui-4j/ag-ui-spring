# ag-ui-spring · spring-server

Spring WebFlux server integration for the [AG-UI protocol](https://docs.ag-ui.com).

It exposes an [`Agent`](https://github.com/ag-ui-4j/ag-ui/blob/main/core/src/main/java/io/github/agui4j/core/agent/Agent.java)
as a reactive `text/event-stream` endpoint and provides the Jackson-based
`Serializer` the protocol needs. It is wire-compatible with the `HttpAgent`
client and is the Spring counterpart to the JDK handler in the `ag-ui` `server`
module.

## What's inside

| Type | Purpose |
|------|---------|
| [`AgUiController`](src/main/java/io/github/agui4j/spring/server/AgUiController.java) | WebFlux `@RestController` that accepts a `RunAgentInput` POST and streams the agent's events as Server-Sent Events. Endpoint path defaults to `/agent` (override with `ag-ui.server.path`). |
| [`JacksonSerializer`](src/main/java/io/github/agui4j/spring/server/JacksonSerializer.java) | A `Serializer` backed by Jackson, configured to handle the sealed `Event` (by `type`) and `Message` (by `role`) hierarchies polymorphically, with `Role`/`EventType` bound to their wire values. |
| [`AgUiServerAutoConfiguration`](src/main/java/io/github/agui4j/spring/server/AgUiServerAutoConfiguration.java) | Spring Boot auto-configuration: contributes a `Serializer` (reusing the app's `ObjectMapper`) and the controller when an `Agent` bean is present. |

## Usage

Define an `Agent` bean; the endpoint is auto-configured:

```java
@Configuration
class AgUiConfig {
    @Bean
    Agent agent() {
        return input -> subscriber -> { /* emit events */ };
    }
}
```

`POST /agent` with a JSON `RunAgentInput` now streams the agent's events as SSE.
Malformed input is rejected with `400 Bad Request`; run failures are surfaced in
band as a terminal `RUN_ERROR` event.

### Wiring manually (without auto-configuration)

```java
Serializer serializer = new JacksonSerializer(objectMapper);
AgUiController controller = new AgUiController(agent, serializer);
```

## Notes

- The `JacksonSerializer` round-trips correctly between this library's client and
  server. Full byte-for-byte parity with the reference TypeScript/Python
  implementations (exact optional-field naming, etc.) may need further mapping
  configuration.

## Dependency

```xml
<dependency>
    <groupId>io.github.ag-ui-4j</groupId>
    <artifactId>ag-ui-spring-server</artifactId>
    <version>3.4.0-SNAPSHOT</version>
</dependency>
```

> This module is versioned independently and tracks the **Spring Boot 3.4.x**
> line it targets. See the [root README](../README.md) for the project overview.
