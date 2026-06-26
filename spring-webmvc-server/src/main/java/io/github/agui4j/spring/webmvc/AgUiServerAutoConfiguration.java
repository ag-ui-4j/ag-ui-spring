package io.github.agui4j.spring.webmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.agui4j.core.agent.Agent;
import io.github.agui4j.core.serialization.Serializer;
import io.github.agui4j.server.AgentRegistry;
import io.github.agui4j.spring.server.core.JacksonSerializer;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that exposes the application's {@link Agent} beans as a
 * Servlet (WebMVC) AG-UI endpoint. Activated only in a Servlet web application;
 * the reactive counterpart lives in {@code ag-ui-spring-webflux-server}.
 *
 * <p>It contributes a {@link JacksonSerializer} (reusing the application's
 * {@link ObjectMapper} if present), a default {@link AgentRegistry} keyed by
 * <em>bean name</em> from all {@link Agent} beans (unless the application defines
 * its own {@link AgentRegistry} bean for friendly ids), and an
 * {@link AgUiController}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class AgUiServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Serializer.class)
    public Serializer agUiSerializer(ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper mapper = objectMapper.getIfAvailable();
        return mapper != null ? new JacksonSerializer(mapper) : new JacksonSerializer();
    }

    @Bean
    @ConditionalOnBean(Agent.class)
    @ConditionalOnMissingBean(AgentRegistry.class)
    public AgentRegistry agUiAgentRegistry(Map<String, Agent> agents) {
        return AgentRegistry.of(agents);
    }

    @Bean
    @ConditionalOnBean(AgentRegistry.class)
    @ConditionalOnMissingBean(AgUiController.class)
    public AgUiController agUiController(AgentRegistry registry, Serializer serializer) {
        return new AgUiController(registry, serializer);
    }
}
