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
package org.example.gcp.slack.claude.common;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.response.Response;
import com.slack.api.bolt.util.SlackRequestParser;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.AppMentionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

/** */
public class Utils {
  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

  private Utils() {}

  public static <K, V> Map<K, List<V>> toMultiMap(MultiValueMap<K, V> multivalueMap) {
    return multivalueMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static Request<?> parseSlackRequest(
      SlackRequestParser requestParser, ServerRequest request, String body) {
    return requestParser.parse(
        SlackRequestParser.HttpRequest.builder()
            .requestUri(request.requestPath().toString())
            .requestBody(body)
            .queryString(toMultiMap(request.queryParams()))
            .remoteAddress(
                request.headers().header("X-Forwarded-For").stream().findFirst().orElse(""))
            .headers(new RequestHeaders(toMultiMap(request.headers().asHttpHeaders())))
            .build());
  }

  public static Mono<Response> processSlackRequest(App slackApp, Request<?> slackRequest) {
    try {
      return Mono.just(slackApp.run(slackRequest));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  public static void sendErrorToSlack(
      EventContext ctx, AppMentionEvent event, String errorMessage) {
    try {
      ctx.client()
          .chatPostMessage(
              r ->
                  r.channel(event.getChannel())
                      .threadTs(event.getThreadTs() != null ? event.getThreadTs() : event.getTs())
                      .text(errorMessage));
      LOG.info("Sent error message to Slack user {}: {}", event.getUser(), errorMessage);
    } catch (IOException | SlackApiException e) {
      LOG.error(
          "Failed to send error message to Slack user {}: {}", event.getUser(), e.getMessage(), e);
    }
  }

  public static String toText(ChatResponse chatResponse) {
    return chatResponse.getResults().stream()
        .map(gen -> gen.getOutput().getText())
        .collect(Collectors.joining("\n"));
  }

  public static String removeMention(String text) {
    return text.replaceFirst("<@.*?>", "").trim();
  }

  public static String threadTs(AppMentionEvent event) {
    return Optional.ofNullable(event.getThreadTs()).orElse(event.getTs());
  }

  public static Message toMessage(String userId, String botId, String text) {
    if (userId.equals(botId)) return new AssistantMessage(text);
    return new UserMessage(text);
  }
}
