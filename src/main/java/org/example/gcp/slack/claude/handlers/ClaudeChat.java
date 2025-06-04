/*
 * Copyright (C) 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.example.gcp.slack.claude.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpSseClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
@Component
public class ClaudeChat {
  private static final Logger LOG = LoggerFactory.getLogger(ClaudeChat.class);

  private final ChatClient.Builder chatClientBuilder;
  private final SystemPromptTemplate systemPrompt;
  private final McpClientCommonProperties mcpCommonProperties;
  private final McpSseClientProperties mcpSseProperties;
  private final ObjectProvider<WebClient.Builder> webClientBuilderProvider;
  private final ObjectProvider<ObjectMapper> objectMapperProvider;

  public ClaudeChat(
      ChatClient.Builder chatClientBuilder,
      SystemPromptTemplate systemPrompt,
      McpClientCommonProperties mcpCommonProperties,
      McpSseClientProperties mcpSseProperties,
      ObjectProvider<WebClient.Builder> webClientBuilder,
      ObjectProvider<ObjectMapper> objectMapper) {
    this.chatClientBuilder = chatClientBuilder;
    this.systemPrompt = systemPrompt;
    this.mcpCommonProperties = mcpCommonProperties;
    this.mcpSseProperties = mcpSseProperties;
    this.webClientBuilderProvider = webClientBuilder;
    this.objectMapperProvider = objectMapper;
  }

  private String connectedClientName(String clientName, String serverConnectionName) {
    return clientName + " - " + serverConnectionName;
  }

  Mono<List<McpAsyncClient>> prepareClients() {
    return Mono.fromCallable(
        () ->
            mcpSseProperties.getConnections().entrySet().stream()
                .map(
                    entry ->
                        new NamedClientMcpTransport(
                            entry.getKey(),
                            WebFluxSseClientTransport.builder(
                                    webClientBuilderProvider
                                        .getIfAvailable(WebClient::builder)
                                        .clone()
                                        .baseUrl(entry.getValue().url()))
                                .sseEndpoint(
                                    Optional.ofNullable(entry.getValue().sseEndpoint())
                                        .orElse("/sse"))
                                .objectMapper(
                                    objectMapperProvider.getIfAvailable(ObjectMapper::new))
                                .build()))
                .map(
                    transport ->
                        McpClient.async(transport.transport())
                            .clientInfo(
                                new McpSchema.Implementation(
                                    connectedClientName(
                                        mcpCommonProperties.getName(), transport.name()),
                                    mcpCommonProperties.getVersion()))
                            .requestTimeout(mcpCommonProperties.getRequestTimeout())
                            .build())
                .map(
                    client -> {
                      client.initialize().block();
                      LOG.info("MCP clients initialized.");
                      return client;
                    })
                .toList());
  }

  Mono<Boolean> cleanup(List<McpAsyncClient> clients) {
    return Mono.fromCallable(
        () -> {
          LOG.info("closing on MCP clients...");
          // clients.stream().map(client -> client.closeGracefully()).toList();
          return true;
        });
  }

  public Flux<String> generate(String message, List<Message> messages) {
    return Flux.usingWhen(
        // McpClients Initialization (resourceAsync)
        prepareClients(),
        // MacpClients usage for Chat client as tools (resourceClosure)
        mcpAsyncClients -> {
          try (var __ = new McpClientAutoConfiguration.CloseableMcpAsyncClients(mcpAsyncClients)) {
            return this.chatClientBuilder
                .clone()
                .defaultToolCallbacks(new AsyncMcpToolCallbackProvider(mcpAsyncClients))
                .build()
                .prompt(
                    new Prompt(
                        Stream.of(
                                List.<Message>of(new UserMessage(message)),
                                messages,
                                List.of(systemPrompt.createMessage()))
                            .flatMap(List::stream)
                            .toList()))
                .stream()
                .content();
          }
        },
        // McpClients cleanup (asyncCleanup)
        mcpAsyncClients -> cleanup(mcpAsyncClients));
  }
}
