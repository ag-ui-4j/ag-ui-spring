/**
 * Framework-agnostic support shared by the AG-UI Spring server modules
 * (WebFlux and WebMVC): a Jackson-backed
 * {@link io.github.agui4j.core.serialization.Serializer} configured for the
 * AG-UI sealed hierarchies, and the {@link
 * io.github.agui4j.spring.server.core.AgentNotFoundException} each controller
 * maps to {@code 404}. This module has no Spring dependency.
 */
package io.github.agui4j.spring.server.core;
