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
package org.example.gcp.slack.claude.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** */
@Configuration
public class AnthropicClaude {

  private final String systemText =
      """
      You are an AI assistant helping people find information and uses the configured tools to do so when necessary.
      You should reply to the people's request considering the responses will be rendered in a Slack channel or conversation.
      Format responses using simple Slack's markup for chats, using: bulleted lists, bold, preformatted blocks and occurrent emojis.
      Everytime you prepare data for your tool's interactions make sure to use supported formats: dates should be in ISO format.
      """;

  @Bean
  public SystemPromptTemplate initSystemTemplate() {
    return new SystemPromptTemplate(systemText);
  }

  @Bean
  public ChatClient setupTools(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
    return chatClientBuilder.defaultToolCallbacks(tools).build();
  }
}
