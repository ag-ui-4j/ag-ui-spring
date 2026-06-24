# ag-ui-spring · spring-ai-spring-boot-starter

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-ui-4j_ag-ui-spring&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-ui-4j_ag-ui-spring)

Spring Boot starter that exposes a Spring AI model as an AG-UI agent over an SSE
endpoint — with **no application code**.

It bundles [`ag-ui-spring-ai`](../spring-ai) and [`ag-ui-spring-server`](../spring-server)
and adds auto-configuration that registers a `SpringAiAgent` from the
auto-configured
[`ChatClient.Builder`](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
(Spring AI provides one whenever a chat model is on the classpath), then lets the
server auto-configuration expose it.

## Usage

```xml
<dependency>
    <groupId>io.github.ag-ui-4j</groupId>
    <artifactId>ag-ui-spring-ai-spring-boot-starter</artifactId>
    <version>1.1.0-SNAPSHOT</version>
</dependency>
```

With a Spring AI model on the classpath (e.g. `spring-ai-starter-model-openai`),
a `ChatClient.Builder` bean is auto-configured by Spring AI; this starter builds a
`ChatClient` from it, turns it into an AG-UI `Agent` and serves it at `/agent`. No
`@Bean` definitions required.

## How it wires up

| Condition | Result |
|-----------|--------|
| A `ChatClient.Builder` bean exists and no `Agent` bean is defined | a `SpringAiAgent` `Agent` bean is registered |
| An `Agent` bean exists (this one or your own) | the `/agent` SSE endpoint is configured |

Enable AG-UI **shared state** (the `update_state` tool and state events) with
properties:

```properties
ag-ui.spring-ai.share-state=true
# Optional: emit STATE_DELTA (RFC 6902 JSON Patch) instead of full STATE_SNAPSHOT
ag-ui.spring-ai.state-updates=DELTA
```

Define your own `Agent` bean to opt out of the auto-configured adapter while
keeping the endpoint. This is also how you customise the `ChatClient` (advisors,
memory, default prompts, registered tools) or state behaviour:

```java
@Bean
Agent agent(ChatClient.Builder builder) {
    return SpringAiAgent.builder(builder.defaultSystem("…").build())
            .shareState(true)
            .build();
}
```

This starter is versioned on the **Spring AI 1.1.x** line. See the
[root README](../README.md) for the project overview.
