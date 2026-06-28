# ag-ui-spring

[![Test](https://github.com/ag-ui-4j/ag-ui-spring/actions/workflows/test.yml/badge.svg)](https://github.com/ag-ui-4j/ag-ui-spring/actions/workflows/test.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-ui-4j_ag-ui-spring&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-ui-4j_ag-ui-spring)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ag-ui-4j_ag-ui-spring&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ag-ui-4j_ag-ui-spring)

Spring integrations for the [**AG-UI protocol**](https://docs.ag-ui.com), built on
top of the framework-agnostic [`ag-ui`](https://github.com/ag-ui-4j/ag-ui) Java
library.

This repository is kept separate so the core `ag-ui` modules stay dependency-light;
the Spring (and Reactor) dependency tree lives only here.

## Modules

| Module | Artifact | Version line | Description |
|--------|----------|--------------|-------------|
| [`spring-server-core`](spring-server-core) | `ag-ui-spring-server-core` | tracks **Spring Boot** (`4.1.x`) | Framework-agnostic code shared by both servers: the Jackson-backed `Serializer` (configured for the AG-UI sealed hierarchies) and `AgentNotFoundException`. No Spring dependency. |
| [`spring-webflux-server`](spring-webflux-server) | `ag-ui-spring-webflux-server` | tracks **Spring Boot** (`4.1.x`) | A reactive **Spring WebFlux** endpoint that streams an `Agent`'s events as Server-Sent Events, plus Spring Boot auto-configuration. Uses the shared serializer from `spring-server-core`. |
| [`spring-webmvc-server`](spring-webmvc-server) | `ag-ui-spring-webmvc-server` | tracks **Spring Boot** (`4.1.x`) | The Servlet (**Spring WebMVC**) equivalent, streaming an `Agent`'s events via an `SseEmitter`. Same routing and shared serializer — pick this if your app is Servlet-based rather than reactive. |
| [`spring-webflux-boot-starter`](spring-webflux-boot-starter) | `ag-ui-spring-webflux-boot-starter` | tracks **Spring Boot** (`4.1.x`) | Drop-in starter over `spring-webflux-server`: add it and define one `Agent` bean to get a working reactive `/agent` endpoint. |
| [`spring-webmvc-boot-starter`](spring-webmvc-boot-starter) | `ag-ui-spring-webmvc-boot-starter` | tracks **Spring Boot** (`4.1.x`) | Drop-in starter over `spring-webmvc-server`: the Servlet equivalent of the WebFlux starter. |
| [`spring-ai`](spring-ai) | `ag-ui-spring-ai` | tracks **Spring AI** (`2.x`) | Adapts a Spring AI `ChatClient` into an AG-UI `Agent`, translating its streamed response into the AG-UI event lifecycle. |
| [`spring-ai-spring-boot-starter`](spring-ai-spring-boot-starter) | `ag-ui-spring-ai-spring-boot-starter` | tracks **Spring AI** (`2.x`) | Zero-code starter (**reactive / WebFlux**): auto-registers a `SpringAiAgent` from the auto-configured `ChatClient.Builder` and exposes it at `/agent`. |
| [`spring-ai-webmvc-boot-starter`](spring-ai-webmvc-boot-starter) | `ag-ui-spring-ai-webmvc-boot-starter` | tracks **Spring AI** (`2.x`) | The **Servlet / WebMVC** equivalent zero-code Spring AI starter: same auto-registration, served over an `SseEmitter`-backed `/agent`. |

## Versioning

The two modules are **versioned and released independently**, because each tracks a
different framework's compatibility:

- the `ag-ui-spring-webflux-server` / `ag-ui-spring-webmvc-server` servers are versioned on the **Spring Boot** line they target (e.g. `4.1.0`);
- `ag-ui-spring-ai` is versioned on the **Spring AI** line it targets (e.g. `2.0.0`).

Each module owns its framework BOM in its own pom, so they can be bumped and
released on separate cadences. The repository's parent pom carries a small,
stable "platform" version (`0.1.0`) for shared configuration only — it does not
force the modules into lockstep. Mix and match the versions you need.

### Releasing

Each line is published to Maven Central on its own tag by the `release` workflow:

| Tag | Publishes |
|-----|-----------|
| `spring-server-core-vX.Y.Z` | `ag-ui-spring-server-core` |
| `spring-webflux-server-vX.Y.Z` | `ag-ui-spring-webflux-server` + `ag-ui-spring-webflux-boot-starter` |
| `spring-webmvc-server-vX.Y.Z` | `ag-ui-spring-webmvc-server` + `ag-ui-spring-webmvc-boot-starter` |
| `spring-ai-vX.Y.Z` | `ag-ui-spring-ai` + `ag-ui-spring-ai-spring-boot-starter` + `ag-ui-spring-ai-webmvc-boot-starter` |

Both server lines depend on `ag-ui-spring-server-core`, so release the
`spring-server-core` line first.

The `release` Maven profile flattens each module's POM (inlining the aggregator
parent), so the lines publish independently. Release the `spring-webflux-server`
line before the `spring-ai` line, since the Spring AI starter depends on the
WebFlux server module — and ensure the `ag-ui` artifacts are on Central first (the Central
Portal rejects SNAPSHOT dependencies).

## Requirements

- **Java 17+**
- **Spring Boot 4.1.x** / **Spring AI 2.x**
- The `ag-ui` artifacts (`io.github.ag-ui-4j:core`, `:server`) — resolved from
  Maven Central (currently `0.1.0`; see `ag-ui.version` in the root POM).

## Quick start

Pick the starter that matches what you have:

**Expose your own agent** — add `ag-ui-spring-webflux-boot-starter` (reactive) or
`ag-ui-spring-webmvc-boot-starter` (Servlet) and define one bean:

```java
@Bean
Agent agent() {
    return input -> subscriber -> { /* emit events */ };
}
```

**Expose a Spring AI model with no code** — add `ag-ui-spring-ai-spring-boot-starter`
(reactive / WebFlux) or `ag-ui-spring-ai-webmvc-boot-starter` (Servlet / WebMVC) and
a Spring AI model (e.g. `spring-ai-starter-model-openai`). A `SpringAiAgent` is
auto-registered from the auto-configured `ChatClient.Builder` and served
automatically.

Either way the endpoint is at `/agent` (override with `ag-ui.server.path`); point
the [`HttpAgent`](https://github.com/ag-ui-4j/ag-ui/tree/main/client) client at it.

## Building

```bash
mvn clean install
```

> Requires the `ag-ui` artifacts in your local repository (or a configured
> repository) first. Until `ag-ui` is published to Maven Central, run
> `mvn install` in the `ag-ui` project.

## Contributing

See the organization's
[Contributing Guide](https://github.com/ag-ui-4j/.github/blob/main/CONTRIBUTING.md)
and [Code of Conduct](https://github.com/ag-ui-4j/.github/blob/main/CODE_OF_CONDUCT.md).

## License

Licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
