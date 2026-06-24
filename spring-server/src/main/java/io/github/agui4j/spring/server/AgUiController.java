package io.github.agui4j.spring.server;

import io.github.agui4j.core.agent.Agent;
import io.github.agui4j.core.agent.RunAgentInput;
import io.github.agui4j.core.event.Event;
import io.github.agui4j.core.event.RunErrorEvent;
import io.github.agui4j.core.serialization.SerializationException;
import io.github.agui4j.core.serialization.Serializer;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

/**
 * A reactive AG-UI endpoint. It accepts a {@code POST} whose JSON body is a
 * {@link RunAgentInput}, runs the configured {@link Agent}, and streams the
 * resulting {@link Event}s back as {@code text/event-stream}.
 *
 * <p>The endpoint path defaults to {@code /agent} and can be overridden with the
 * {@code ag-ui.server.path} property. This is the Spring WebFlux counterpart to
 * the JDK-based handler in the {@code server} module and is wire-compatible with
 * the {@code HttpAgent} client.
 */
@RestController
public class AgUiController {

    private final Agent agent;
    private final Serializer serializer;

    /**
     * Creates the controller.
     *
     * @param agent      the agent to run for each request (required)
     * @param serializer the serializer used to read input and encode events
     *                   (required)
     */
    public AgUiController(Agent agent, Serializer serializer) {
        this.agent = Objects.requireNonNull(agent, "agent must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
    }

    /**
     * Runs the agent and streams its events.
     *
     * @param body the JSON-encoded {@link RunAgentInput}
     * @return the agent's events as Server-Sent Events
     */
    @PostMapping(value = "${ag-ui.server.path:/agent}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> run(@RequestBody String body) {
        RunAgentInput input = serializer.deserialize(body, RunAgentInput.class);
        return JdkFlowAdapter.flowPublisherToFlux(agent.run(input))
                // Surface run failures in band as a terminal RUN_ERROR event,
                // matching the protocol rather than abruptly closing the stream.
                .onErrorResume(throwable -> Flux.just((Event) new RunErrorEvent(describe(throwable))))
                .map(event -> ServerSentEvent.builder(serializer.serialize(event)).build());
    }

    @ExceptionHandler(SerializationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String onMalformedRequest(SerializationException e) {
        return "Invalid AG-UI request: " + e.getMessage();
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
