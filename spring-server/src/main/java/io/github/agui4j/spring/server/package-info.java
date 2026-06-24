/**
 * Spring WebFlux integration for the AG-UI protocol: a reactive controller that
 * streams an {@link io.github.agui4j.core.agent.Agent}'s events as Server-Sent
 * Events, a Jackson-backed {@link io.github.agui4j.core.serialization.Serializer}
 * configured for the AG-UI sealed hierarchies, and Spring Boot auto-configuration
 * to wire them together.
 */
package io.github.agui4j.spring.server;
