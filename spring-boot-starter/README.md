# ag-ui-spring · spring-boot-starter

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-ui-4j_ag-ui-spring&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-ui-4j_ag-ui-spring)

Spring Boot starter that exposes an AG-UI
[`Agent`](https://github.com/ag-ui-4j/ag-ui/blob/main/core/src/main/java/io/github/agui4j/core/agent/Agent.java)
over an HTTP Server-Sent Events endpoint.

Add this single dependency and define one `Agent` bean — auto-configuration
(from [`ag-ui-spring-server`](../spring-server)) wires a `Serializer` and the
`/agent` endpoint around it.

## Usage

```xml
<dependency>
    <groupId>io.github.ag-ui-4j</groupId>
    <artifactId>ag-ui-spring-boot-starter</artifactId>
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
Override the path with `ag-ui.server.path`.

> Use this when you bring your own `Agent`. To expose a **Spring AI** model with
> no code, use [`ag-ui-spring-ai-spring-boot-starter`](../spring-ai-spring-boot-starter)
> instead.

This starter is versioned on the **Spring Boot 3.4.x** line. See the
[root README](../README.md) for the project overview.
