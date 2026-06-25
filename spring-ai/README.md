# ag-ui-spring · spring-ai

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-ui-4j_ag-ui-spring&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-ui-4j_ag-ui-spring)

Spring AI integration for the [AG-UI protocol](https://docs.ag-ui.com).

It adapts a Spring AI
[`ChatClient`](https://docs.spring.io/spring-ai/reference/api/chatclient.html) into
an AG-UI
[`Agent`](https://github.com/ag-ui-4j/ag-ui/blob/main/core/src/main/java/io/github/agui4j/core/agent/Agent.java),
so any Spring AI model (OpenAI, Ollama, …) can drive an AG-UI front end. The
`ChatClient` is the high-level entry point (advisors, memory, default prompts,
registered tools) and is built over any `ChatModel`.

## What's inside

| Type | Purpose |
|------|---------|
| [`SpringAiAgent`](src/main/java/io/github/agui4j/spring/ai/SpringAiAgent.java) | Wraps a `ChatClient`. Maps the `RunAgentInput` conversation onto a Spring AI prompt, streams the response (`Flux<ChatResponse>`), and emits the AG-UI event lifecycle. |

## Event mapping

A run produces the following AG-UI events:

```
RUN_STARTED
  REASONING_START                     (for <think>...</think> content)
    REASONING_MESSAGE_START
      REASONING_MESSAGE_CONTENT*
    REASONING_MESSAGE_END
  REASONING_END
  TEXT_MESSAGE_START                  (when the model produces text)
    TEXT_MESSAGE_CONTENT*
  TEXT_MESSAGE_END
  TOOL_CALL_START                     (per tool call the model requests)
    TOOL_CALL_ARGS*
  TOOL_CALL_END
RUN_FINISHED
```

**Reasoning** is detected from inline `<think>...</think>` tags in the streamed
text (the provider-agnostic convention reasoning models use, and the reason
Spring AI ships a `ThinkingTagCleaner`). Content inside the tags is emitted as
the reasoning sub-stream; content outside is emitted as text. Tags split across
streaming chunks are reassembled. Models that don't use think tags are
unaffected — all content is emitted as text.

The agent owns the **tool-execution loop**, distinguishing two kinds of tools:

- **Client-side tools** — those on the `RunAgentInput`. Advertised to the model
  and surfaced as `TOOL_CALL_START/ARGS/END` events for the front end to execute;
  the agent does not run them.
- **Backend tools** — registered with `SpringAiAgent.builder(client).tools(...)`.
  When the model calls one, the agent emits `TOOL_CALL_START/ARGS/END`, **executes
  it** (via Spring AI's `ToolCallingManager`), emits a **`TOOL_CALL_RESULT`**, then
  re-prompts the model with the result — looping until the model stops calling
  backend tools. If a turn mixes backend and client calls, the backend results are
  emitted and the run stops so the front end handles the client calls.

**Tool-call mapping**: streaming argument chunks are correlated by tool-call id;
the first chunk carrying an id (or a name) opens the call, later chunks append
argument deltas. Providers that omit the id (e.g. Ollama) get a synthesized one.
Any open text message is closed before a tool call starts.

**State** (opt-in) syncs AG-UI [shared state](https://docs.ag-ui.com/concepts/state).
When enabled the agent:

- emits an initial `STATE_SNAPSHOT` echoing `RunAgentInput.state` at run start;
- injects the current state into the prompt and advertises an `update_state`
  tool (taking the complete new state);
- intercepts the model's call to that tool and emits the new state — the call is
  **not** surfaced as a `TOOL_CALL_*` event, and it is not executed server-side.

State changes are emitted as a full `STATE_SNAPSHOT` by default, or as a
`STATE_DELTA` (RFC 6902 JSON Patch, diffed against the run's input state) when
configured with `StateUpdates.DELTA`. The model always supplies the complete new
state; the agent computes the delta, so it stays reliable regardless of the
model's ability to produce JSON Patch.

If the model errors, a terminal `RUN_ERROR` event is emitted instead of
propagating the failure — matching the protocol's in-band error handling.

## Usage

Build a `ChatClient` over any `ChatModel` — add advisors, chat memory, default
prompts and registered tools as needed:

```java
ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultSystem("You are a helpful assistant.")
        .defaultTools(myTools)
        .build();
Agent agent = new SpringAiAgent(chatClient);
```

For a quick start, `ChatClient.create(chatModel)` builds a client with defaults.

To enable AG-UI shared state, use the builder:

```java
Agent agent = SpringAiAgent.builder(chatClient)
        .shareState(true)
        .stateUpdates(SpringAiAgent.StateUpdates.DELTA)   // optional; default is SNAPSHOT
        .build();
```

Combine with `ag-ui-spring-webflux-server` to expose it over HTTP. The
[`ag-ui-spring-ai-spring-boot-starter`](../spring-ai-spring-boot-starter) does this
automatically from the auto-configured `ChatClient.Builder`; to customise the
client, define the `Agent` bean yourself (which overrides the auto-configured
one):

```java
@Bean
Agent agent(ChatClient.Builder builder) {
    return new SpringAiAgent(builder.defaultSystem("…").build());
}
```

## Scope

Advertises the **input tools** to the model and maps streamed **text**,
**reasoning** (`<think>` tags), **tool calls** and — when state sharing is
enabled — **shared state** (`STATE_SNAPSHOT` or `STATE_DELTA`) to AG-UI events.

`MESSAGES_SNAPSHOT` is not yet emitted. Tool-call **results**
(`TOOL_CALL_RESULT`) are produced by the side executing the tool, not by this
streaming adapter.

## Dependency

```xml
<dependency>
    <groupId>io.github.ag-ui-4j</groupId>
    <artifactId>ag-ui-spring-ai</artifactId>
    <version>1.1.0-SNAPSHOT</version>
</dependency>
```

> This module is versioned independently and tracks the **Spring AI 1.1.x** line
> it targets. See the [root README](../README.md) for the project overview.
