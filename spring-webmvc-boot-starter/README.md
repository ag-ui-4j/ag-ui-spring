# ag-ui-spring · spring-webmvc-boot-starter

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-ui-4j_ag-ui-spring&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-ui-4j_ag-ui-spring)

Spring Boot starter that exposes an AG-UI
[`Agent`](https://github.com/ag-ui-4j/ag-ui/blob/main/core/src/main/java/io/github/agui4j/core/agent/Agent.java)
over a Servlet (**WebMVC**) HTTP Server-Sent Events endpoint.

Add this single dependency and define one `Agent` bean — auto-configuration
(from [`ag-ui-spring-webmvc-server`](../spring-webmvc-server)) wires a `Serializer`
and the `SseEmitter`-backed `/agent` endpoint around it. For a reactive (WebFlux)
app, use [`ag-ui-spring-webflux-boot-starter`](../spring-webflux-boot-starter)
instead.

## Usage

```xml
<dependency>
    <groupId>io.github.ag-ui-4j</groupId>
    <artifactId>ag-ui-spring-webmvc-boot-starter</artifactId>
    <version>3.4.0-SNAPSHOT</version>
</dependency>
```

```java
@Bean
Agent agent() {
    return input -> subscriber -> { /* emit events */ };
}
```

`POST /agent` with a JSON `RunAgentInput` now streams the agent's events as SSE.
Several `Agent` beans are each reachable at `/agent/{beanName}`; override the base
path with `ag-ui.server.path`.

This starter is versioned on the **Spring Boot 3.4.x** line. See the
[root README](../README.md) for the project overview.
