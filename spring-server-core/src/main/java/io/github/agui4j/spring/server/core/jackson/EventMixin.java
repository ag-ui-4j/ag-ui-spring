package io.github.agui4j.spring.server.core.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.agui4j.core.event.ActivityDeltaEvent;
import io.github.agui4j.core.event.ActivitySnapshotEvent;
import io.github.agui4j.core.event.CustomEvent;
import io.github.agui4j.core.event.MessagesSnapshotEvent;
import io.github.agui4j.core.event.MetaEvent;
import io.github.agui4j.core.event.RawEvent;
import io.github.agui4j.core.event.ReasoningEncryptedValueEvent;
import io.github.agui4j.core.event.ReasoningEndEvent;
import io.github.agui4j.core.event.ReasoningMessageChunkEvent;
import io.github.agui4j.core.event.ReasoningMessageContentEvent;
import io.github.agui4j.core.event.ReasoningMessageEndEvent;
import io.github.agui4j.core.event.ReasoningMessageStartEvent;
import io.github.agui4j.core.event.ReasoningStartEvent;
import io.github.agui4j.core.event.RunErrorEvent;
import io.github.agui4j.core.event.RunFinishedEvent;
import io.github.agui4j.core.event.RunStartedEvent;
import io.github.agui4j.core.event.StateDeltaEvent;
import io.github.agui4j.core.event.StateSnapshotEvent;
import io.github.agui4j.core.event.StepFinishedEvent;
import io.github.agui4j.core.event.StepStartedEvent;
import io.github.agui4j.core.event.TextMessageChunkEvent;
import io.github.agui4j.core.event.TextMessageContentEvent;
import io.github.agui4j.core.event.TextMessageEndEvent;
import io.github.agui4j.core.event.TextMessageStartEvent;
import io.github.agui4j.core.event.ToolCallArgsEvent;
import io.github.agui4j.core.event.ToolCallChunkEvent;
import io.github.agui4j.core.event.ToolCallEndEvent;
import io.github.agui4j.core.event.ToolCallResultEvent;
import io.github.agui4j.core.event.ToolCallStartEvent;

/**
 * Jackson mix-in that maps the {@link io.github.agui4j.core.event.Event} sealed
 * hierarchy to its {@code type} discriminator on the wire. Applied to
 * {@code Event} via {@code ObjectMapper#addMixIn} so the {@code core} module
 * stays free of Jackson annotations.
 *
 * <p>The subtype names match {@link io.github.agui4j.core.event.EventType}.
 * Jackson writes and reads the {@code type} property itself, so it does not rely
 * on the (computed) {@code type()} accessor being exposed as a bean property.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = RunStartedEvent.class, name = "RUN_STARTED"),
        @Type(value = RunFinishedEvent.class, name = "RUN_FINISHED"),
        @Type(value = RunErrorEvent.class, name = "RUN_ERROR"),
        @Type(value = StepStartedEvent.class, name = "STEP_STARTED"),
        @Type(value = StepFinishedEvent.class, name = "STEP_FINISHED"),
        @Type(value = TextMessageStartEvent.class, name = "TEXT_MESSAGE_START"),
        @Type(value = TextMessageContentEvent.class, name = "TEXT_MESSAGE_CONTENT"),
        @Type(value = TextMessageEndEvent.class, name = "TEXT_MESSAGE_END"),
        @Type(value = TextMessageChunkEvent.class, name = "TEXT_MESSAGE_CHUNK"),
        @Type(value = ToolCallStartEvent.class, name = "TOOL_CALL_START"),
        @Type(value = ToolCallArgsEvent.class, name = "TOOL_CALL_ARGS"),
        @Type(value = ToolCallEndEvent.class, name = "TOOL_CALL_END"),
        @Type(value = ToolCallChunkEvent.class, name = "TOOL_CALL_CHUNK"),
        @Type(value = ToolCallResultEvent.class, name = "TOOL_CALL_RESULT"),
        @Type(value = ReasoningStartEvent.class, name = "REASONING_START"),
        @Type(value = ReasoningEndEvent.class, name = "REASONING_END"),
        @Type(value = ReasoningMessageStartEvent.class, name = "REASONING_MESSAGE_START"),
        @Type(value = ReasoningMessageContentEvent.class, name = "REASONING_MESSAGE_CONTENT"),
        @Type(value = ReasoningMessageEndEvent.class, name = "REASONING_MESSAGE_END"),
        @Type(value = ReasoningMessageChunkEvent.class, name = "REASONING_MESSAGE_CHUNK"),
        @Type(value = ReasoningEncryptedValueEvent.class, name = "REASONING_ENCRYPTED_VALUE"),
        @Type(value = StateSnapshotEvent.class, name = "STATE_SNAPSHOT"),
        @Type(value = StateDeltaEvent.class, name = "STATE_DELTA"),
        @Type(value = MessagesSnapshotEvent.class, name = "MESSAGES_SNAPSHOT"),
        @Type(value = ActivitySnapshotEvent.class, name = "ACTIVITY_SNAPSHOT"),
        @Type(value = ActivityDeltaEvent.class, name = "ACTIVITY_DELTA"),
        @Type(value = RawEvent.class, name = "RAW"),
        @Type(value = CustomEvent.class, name = "CUSTOM"),
        @Type(value = MetaEvent.class, name = "META_EVENT"),
})
public interface EventMixin {
}
