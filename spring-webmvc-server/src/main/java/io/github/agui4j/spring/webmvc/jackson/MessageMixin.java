package io.github.agui4j.spring.webmvc.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.agui4j.core.message.AssistantMessage;
import io.github.agui4j.core.message.DeveloperMessage;
import io.github.agui4j.core.message.SystemMessage;
import io.github.agui4j.core.message.ToolMessage;
import io.github.agui4j.core.message.UserMessage;

/**
 * Jackson mix-in that maps the {@link io.github.agui4j.core.message.Message}
 * sealed hierarchy to its {@code role} discriminator on the wire. The subtype
 * names match the wire values of {@link io.github.agui4j.core.message.Role}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "role")
@JsonSubTypes({
        @Type(value = DeveloperMessage.class, name = "developer"),
        @Type(value = SystemMessage.class, name = "system"),
        @Type(value = AssistantMessage.class, name = "assistant"),
        @Type(value = UserMessage.class, name = "user"),
        @Type(value = ToolMessage.class, name = "tool"),
})
public interface MessageMixin {
}
