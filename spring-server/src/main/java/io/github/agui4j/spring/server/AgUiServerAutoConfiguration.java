package io.github.agui4j.spring.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.agui4j.core.agent.Agent;
import io.github.agui4j.core.serialization.Serializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Auto-configuration that exposes a configured {@link Agent} as an AG-UI
 * endpoint. It contributes:
 *
 * <ul>
 *   <li>a {@link JacksonSerializer} (reusing the application's
 *       {@link ObjectMapper} if one is present) when no {@link Serializer} bean
 *       exists; and</li>
 *   <li>an {@link AgUiController} when an {@link Agent} bean is present.</li>
 * </ul>
 *
 * <p>A consumer therefore only needs to define a single {@code Agent} bean to
 * obtain a working {@code /agent} endpoint.
 */
@AutoConfiguration
public class AgUiServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Serializer.class)
    public Serializer agUiSerializer(ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper mapper = objectMapper.getIfAvailable();
        return mapper != null ? new JacksonSerializer(mapper) : new JacksonSerializer();
    }

    @Bean
    @ConditionalOnBean(Agent.class)
    @ConditionalOnMissingBean(AgUiController.class)
    public AgUiController agUiController(Agent agent, Serializer serializer) {
        return new AgUiController(agent, serializer);
    }
}
