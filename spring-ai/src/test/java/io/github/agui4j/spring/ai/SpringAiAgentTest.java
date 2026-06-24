package io.github.agui4j.spring.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.agui4j.core.agent.RunAgentInput;
import io.github.agui4j.core.event.Event;
import io.github.agui4j.core.event.EventType;
import io.github.agui4j.core.event.JsonPatchOperation;
import io.github.agui4j.core.event.RunErrorEvent;
import io.github.agui4j.core.event.StateDeltaEvent;
import io.github.agui4j.core.event.StateSnapshotEvent;
import io.github.agui4j.core.event.TextMessageContentEvent;
import io.github.agui4j.core.event.ToolCallResultEvent;
import io.github.agui4j.core.message.UserMessage;
import io.github.agui4j.core.tool.Tool;
import io.github.agui4j.core.tool.ToolParameters;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

/**
 * End-to-end tests that drive the agent through a real {@link ChatClient} (built
 * over a fake {@link ChatModel}). The detailed chunk-to-event mapping is covered
 * by {@link SpringAiEventTranslatorTest}.
 */
class SpringAiAgentTest {

    private static final RunAgentInput INPUT = new RunAgentInput("t1", "r1",
            List.of(new UserMessage("m1", "hi")), List.of());

    @Test
    void wrapsTheModelResponseInTheRunLifecycle() {
        ChatClient chatClient = ChatClient.create(streaming(chunk("hello")));
        SpringAiAgent agent = new SpringAiAgent(chatClient, () -> "msg-1");

        List<Event> events = collect(agent.run(INPUT));

        assertEquals(EventType.RUN_STARTED, events.get(0).type());
        assertEquals(EventType.RUN_FINISHED, events.get(events.size() - 1).type());
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.TEXT_MESSAGE_START));
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.TEXT_MESSAGE_END));

        String text = events.stream()
                .filter(e -> e instanceof TextMessageContentEvent)
                .map(e -> ((TextMessageContentEvent) e).delta())
                .reduce("", String::concat);
        assertEquals("hello", text);
    }

    @Test
    void streamsTextFromAnAsynchronousModel() {
        ChatModel asyncModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                // Emit on another thread, as a real model (Ollama) does.
                return Flux.just(chunk("Hello "), chunk("world"))
                        .delayElements(java.time.Duration.ofMillis(10));
            }
        };
        SpringAiAgent agent = new SpringAiAgent(ChatClient.create(asyncModel), () -> "msg-1");

        List<Event> events = collect(agent.run(INPUT));

        String text = events.stream()
                .filter(e -> e instanceof TextMessageContentEvent)
                .map(e -> ((TextMessageContentEvent) e).delta())
                .reduce("", String::concat);
        assertEquals("Hello world", text);
    }

    @Test
    void surfacesModelFailureAsRunError() {
        ChatModel failing = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new IllegalStateException("model exploded");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.error(new IllegalStateException("model exploded"));
            }
        };
        SpringAiAgent agent = new SpringAiAgent(ChatClient.create(failing), () -> "msg-1");

        List<Event> events = collect(agent.run(INPUT));
        Event last = events.get(events.size() - 1);

        RunErrorEvent error = assertInstanceOf(RunErrorEvent.class, last);
        assertTrue(error.message().contains("model exploded"), error.message());
    }

    @Test
    void advertisesRunInputToolsToTheModelWithoutInternalExecution() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        ChatModel capturing = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                captured.set(prompt);
                return Flux.just(chunk("ok"));
            }
        };
        Tool tool = new Tool("get_weather", "Get the weather for a city",
                new ToolParameters(Map.of("city", Map.of("type", "string")), List.of("city")));
        RunAgentInput input = new RunAgentInput("t1", "r1",
                List.of(new UserMessage("m1", "weather?")), List.of(tool));

        SpringAiAgent agent = new SpringAiAgent(ChatClient.create(capturing), () -> "msg-1");
        collect(agent.run(input));

        Prompt prompt = captured.get();
        assertNotNull(prompt, "model should have been invoked");
        ToolCallingChatOptions options =
                assertInstanceOf(ToolCallingChatOptions.class, prompt.getOptions());
        assertEquals(Boolean.FALSE, options.getInternalToolExecutionEnabled());

        List<String> toolNames = options.getToolCallbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();
        assertTrue(toolNames.contains("get_weather"), toolNames.toString());

        String schema = options.getToolCallbacks().stream()
                .filter(callback -> callback.getToolDefinition().name().equals("get_weather"))
                .map(callback -> callback.getToolDefinition().inputSchema())
                .findFirst()
                .orElseThrow();
        assertTrue(schema.contains("\"city\""), schema);
    }

    @Test
    void emitsInitialStateSnapshotAndAdvertisesStateToolWhenSharingEnabled() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        ChatModel capturing = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                captured.set(prompt);
                return Flux.just(chunk("ok"));
            }
        };
        RunAgentInput input = new RunAgentInput("t1", "r1", Map.of("count", 1),
                List.of(new UserMessage("m1", "hi")), List.of(), List.of(), null);
        SpringAiAgent agent = SpringAiAgent.builder(ChatClient.create(capturing))
                .messageIdGenerator(() -> "msg-1")
                .shareState(true)
                .build();

        List<Event> events = collect(agent.run(input));

        // Initial snapshot is emitted right after RUN_STARTED.
        assertEquals(EventType.RUN_STARTED, events.get(0).type());
        StateSnapshotEvent snapshot = assertInstanceOf(StateSnapshotEvent.class, events.get(1));
        assertEquals(Map.of("count", 1), snapshot.snapshot());

        // The update_state tool is advertised to the model.
        ToolCallingChatOptions options =
                assertInstanceOf(ToolCallingChatOptions.class, captured.get().getOptions());
        List<String> toolNames = options.getToolCallbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();
        assertTrue(toolNames.contains("update_state"), toolNames.toString());
    }

    @Test
    void emitsNoStateEventsWhenSharingDisabled() {
        ChatClient chatClient = ChatClient.create(streaming(chunk("ok")));
        RunAgentInput input = new RunAgentInput("t1", "r1", Map.of("count", 1),
                List.of(new UserMessage("m1", "hi")), List.of(), List.of(), null);
        SpringAiAgent agent = new SpringAiAgent(chatClient, () -> "msg-1");

        List<Event> events = collect(agent.run(input));

        assertTrue(events.stream().noneMatch(e -> e.type() == EventType.STATE_SNAPSHOT));
    }

    @Test
    void injectsStateIntoThePromptByDefault() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        RunAgentInput input = new RunAgentInput("t1", "r1", Map.of("count", 7),
                List.of(new UserMessage("m1", "hi")), List.of(), List.of(), null);
        SpringAiAgent agent = SpringAiAgent.builder(ChatClient.create(capturingModel(captured)))
                .messageIdGenerator(() -> "msg-1")
                .shareState(true)
                .build();

        collect(agent.run(input));

        String prompt = captured.get().getContents();
        assertTrue(prompt.contains("count"), prompt);
        assertTrue(prompt.contains("update_state"), prompt);
    }

    @Test
    void usesACustomStatePromptWhenConfigured() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        RunAgentInput input = new RunAgentInput("t1", "r1", Map.of("count", 7),
                List.of(new UserMessage("m1", "hi")), List.of(), List.of(), null);
        SpringAiAgent agent = SpringAiAgent.builder(ChatClient.create(capturingModel(captured)))
                .messageIdGenerator(() -> "msg-1")
                .shareState(true)
                .statePrompt(state -> "SHARED_STATE_MARKER " + state)
                .build();

        collect(agent.run(input));

        assertTrue(captured.get().getContents().contains("SHARED_STATE_MARKER"), captured.get().getContents());
    }

    @Test
    void emitsStateDeltaWhenConfiguredForDeltaUpdates() {
        ChatModel model = streaming(toolChunk("call-1", "update_state", "{\"state\":{\"count\":2}}"));
        RunAgentInput input = new RunAgentInput("t1", "r1", Map.of("count", 1),
                List.of(new UserMessage("m1", "increment")), List.of(), List.of(), null);
        SpringAiAgent agent = SpringAiAgent.builder(ChatClient.create(model))
                .messageIdGenerator(() -> "msg-1")
                .shareState(true)
                .stateUpdates(SpringAiAgent.StateUpdates.DELTA)
                .build();

        List<Event> events = collect(agent.run(input));

        // Initial baseline is still a snapshot; the model's update becomes a delta.
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.STATE_SNAPSHOT));
        StateDeltaEvent delta = (StateDeltaEvent) events.stream()
                .filter(e -> e.type() == EventType.STATE_DELTA)
                .findFirst()
                .orElseThrow();
        assertEquals(1, delta.delta().size());
        JsonPatchOperation op = delta.delta().get(0);
        assertEquals("replace", op.op());
        assertEquals("/count", op.path());
        assertEquals(2, ((Number) op.value()).intValue());
    }

    private static ChatResponse toolChunk(String id, String name, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    @Test
    void executesBackendToolEmitsResultAndContinues() {
        ToolCallback weather = backendTool("getWeather", "{\"temperature\":21}");
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                boolean afterToolResult = prompt.getInstructions().stream()
                        .anyMatch(m -> m instanceof ToolResponseMessage);
                if (afterToolResult) {
                    return Flux.just(chunk("It is 21 degrees in Paris."));
                }
                AssistantMessage toolCall = AssistantMessage.builder()
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "call-1", "function", "getWeather", "{\"location\":\"Paris\"}")))
                        .build();
                return Flux.just(new ChatResponse(List.of(new Generation(toolCall))));
            }
        };
        SpringAiAgent agent = SpringAiAgent.builder(ChatClient.create(model))
                .messageIdGenerator(sequentialIds())
                .tools(List.of(weather))
                .build();

        List<Event> events = collect(agent.run(INPUT));

        assertEquals(
                List.of(
                        EventType.RUN_STARTED,
                        EventType.TOOL_CALL_START,
                        EventType.TOOL_CALL_ARGS,
                        EventType.TOOL_CALL_END,
                        EventType.TOOL_CALL_RESULT,
                        EventType.TEXT_MESSAGE_START,
                        EventType.TEXT_MESSAGE_CONTENT,
                        EventType.TEXT_MESSAGE_END,
                        EventType.RUN_FINISHED),
                events.stream().map(Event::type).toList());

        ToolCallResultEvent result = (ToolCallResultEvent) events.stream()
                .filter(e -> e.type() == EventType.TOOL_CALL_RESULT)
                .findFirst()
                .orElseThrow();
        assertEquals("call-1", result.toolCallId());
        assertTrue(result.content().contains("21"), result.content());
    }

    @Test
    void emitsBackendResultThenStopsWhenAClientToolIsAlsoCalled() {
        ToolCallback weather = backendTool("getWeather", "{\"temperature\":21}");
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                AssistantMessage toolCalls = AssistantMessage.builder()
                        .toolCalls(List.of(
                                new AssistantMessage.ToolCall("call-1", "function", "getWeather", "{}"),
                                new AssistantMessage.ToolCall("call-2", "function", "showDialog", "{}")))
                        .build();
                return Flux.just(new ChatResponse(List.of(new Generation(toolCalls))));
            }
        };
        // showDialog is a client-side tool from the run input.
        Tool clientTool = new Tool("showDialog", "Show a dialog",
                new ToolParameters(Map.of(), List.of()));
        RunAgentInput input = new RunAgentInput("t1", "r1",
                List.of(new UserMessage("m1", "hi")), List.of(clientTool));
        SpringAiAgent agent = SpringAiAgent.builder(ChatClient.create(model))
                .messageIdGenerator(sequentialIds())
                .tools(List.of(weather))
                .build();

        List<Event> events = collect(agent.run(input));

        // The backend result is emitted, but there is no second (text) turn — the
        // run stops so the front end can run the client tool call.
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.TOOL_CALL_RESULT));
        assertTrue(events.stream().noneMatch(e -> e.type() == EventType.TEXT_MESSAGE_CONTENT));
        assertEquals(EventType.RUN_FINISHED, events.get(events.size() - 1).type());
    }

    private static Supplier<String> sequentialIds() {
        AtomicInteger counter = new AtomicInteger();
        return () -> "m" + counter.incrementAndGet();
    }

    private static ToolCallback backendTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name(name)
                        .description("test backend tool")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return result;
            }
        };
    }

    private static ChatModel capturingModel(AtomicReference<Prompt> captured) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                captured.set(prompt);
                return Flux.just(chunk("ok"));
            }
        };
    }

    private static List<Event> collect(java.util.concurrent.Flow.Publisher<Event> publisher) {
        return JdkFlowAdapter.flowPublisherToFlux(publisher).collectList().block();
    }

    private static ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static ChatModel streaming(ChatResponse... chunks) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return chunks.length > 0 ? chunks[chunks.length - 1] : new ChatResponse(List.of());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.fromArray(chunks);
            }
        };
    }
}
