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

import java.util.List;
import java.util.stream.Stream;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** */
@Component
public class ClaudeChat {
  private final ChatClient chatClient;
  private final SystemPromptTemplate systemPrompt;

  public ClaudeChat(ChatClient chatClient, SystemPromptTemplate systemPrompt) {
    this.chatClient = chatClient;
    this.systemPrompt = systemPrompt;
  }

  public Flux<String> generate(String message, List<Message> messages) {
    return chatClient
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
}
