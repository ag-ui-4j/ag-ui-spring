package io.github.agui4j.spring.ai.boot;

import io.github.agui4j.core.agent.Agent;
import io.github.agui4j.spring.ai.SpringAiAgent;
import io.github.agui4j.spring.server.AgUiServerAutoConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that adapts a Spring AI {@link ChatClient} into an AG-UI
 * {@link Agent}. When a {@link ChatClient.Builder} bean is present (Spring AI
 * auto-configures one whenever a chat model is on the classpath) and the
 * application has not defined its own {@code Agent}, it builds a default
 * {@link ChatClient} and registers a {@link SpringAiAgent}.
 *
 * <p>Ordering matters: it runs <em>after</em> Spring AI's
 * {@code ChatClientAutoConfiguration} (which registers the {@code ChatClient.Builder}
 * this configuration consumes) and <em>before</em> {@link AgUiServerAutoConfiguration}
 * (so the contributed agent exists when the server wires the AG-UI endpoint).
 * Together they expose a Spring AI model over AG-UI with no application code. To
 * customise the client (advisors, memory, default prompts, tools), define your
 * own {@code Agent} bean from a {@code ChatClient.Builder}.
 *
 * <p>Set {@code ag-ui.spring-ai.share-state=true} to enable AG-UI shared state
 * (the {@code update_state} tool and state events). With
 * {@code ag-ui.spring-ai.state-updates=DELTA}, state changes are emitted as a
 * {@code STATE_DELTA} (RFC 6902 JSON Patch) instead of a full {@code STATE_SNAPSHOT}.
 */
@AutoConfiguration(
        afterName = "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        before = AgUiServerAutoConfiguration.class)
@ConditionalOnClass({ChatClient.class, SpringAiAgent.class})
public class AgUiSpringAiAutoConfiguration {

    @Bean
    @ConditionalOnBean(ChatClient.Builder.class)
    @ConditionalOnMissingBean(Agent.class)
    public Agent springAiAgent(ChatClient.Builder chatClientBuilder,
            @Value("${ag-ui.spring-ai.share-state:false}") boolean shareState,
            @Value("${ag-ui.spring-ai.state-updates:SNAPSHOT}") SpringAiAgent.StateUpdates stateUpdates) {
        return SpringAiAgent.builder(chatClientBuilder.build())
                .shareState(shareState)
                .stateUpdates(stateUpdates)
                .build();
    }
}
