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

import static org.example.gcp.slack.claude.common.Utils.removeMention;
import static org.example.gcp.slack.claude.common.Utils.sendErrorToSlack;
import static org.example.gcp.slack.claude.common.Utils.threadTs;
import static org.example.gcp.slack.claude.common.Utils.exceptionMessage;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.Event;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageEvent;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

/** */
@Component
public class SlackEvent {
  private static final Logger LOG = LoggerFactory.getLogger(SlackEvent.class);

  private final ClaudeChat claude;
  private final SlackOperations slack;

  public SlackEvent(ClaudeChat claude, SlackOperations send) {
    this.claude = claude;
    this.slack = send;
  }

  public Response mention(EventsApiPayload<AppMentionEvent> payload, EventContext ctx) {
    var event = payload.getEvent();
    var userMessageText = removeMention(event.getText());
    var channelId = event.getChannel();
    var threadTs = threadTs(event);

    LOG.info(
        "Received app_mention event from user {} in channel {}: {}",
        event.getUser(),
        channelId,
        userMessageText);

    process(ctx, event, channelId, threadTs, userMessageText);

    return ctx.ack();
  }

  public Response threadMessage(EventsApiPayload<MessageEvent> payload, EventContext ctx) {
    var event = payload.getEvent();
    if (event.getThreadTs() != null) {
      var userMessageText = event.getText();
      var channelId = event.getChannel();
      var threadTs = event.getThreadTs();
      LOG.info(
          "Received message event on thread {} from user {} (is bot: {}) in channel {}: {}",
          threadTs,
          event.getUser(),
          event.getUser().equals(ctx.getBotUserId()),
          channelId,
          userMessageText);

      process(ctx, event, channelId, threadTs, userMessageText);
    }
    return ctx.ack();
  }

  public Response threadMessageChange(
      EventsApiPayload<MessageChangedEvent> payload, EventContext ctx) {
    var event = payload.getEvent();
    var message = event.getMessage();
    if (message.getThreadTs() != null) {
      var userMessageText = message.getText();
      var channelId = event.getChannel();
      var threadTs = message.getThreadTs();
      LOG.info(
          "Received changed message event on thread {} from user {} (is bot: {}) in channel {}: {}",
          threadTs,
          message.getUser(),
          message.getUser().equals(ctx.getBotUserId()),
          channelId,
          userMessageText);

      process(ctx, event, channelId, threadTs, userMessageText);
    }
    return ctx.ack();
  }

  void process(EventContext ctx, Event event, String channelId, String threadTs, String message) {
    slack
        .reply(ctx, event, "Thinking...")
        .flatMap(__ -> slack.history(ctx, channelId, threadTs))
        .flatMapMany(previousMessages -> claude.generate(message, previousMessages))
        .collectList()
        .flatMap(
            llmResponse ->
                slack.reply(ctx, event, llmResponse.stream().collect(Collectors.joining("\n"))))
        .subscribe(
            __ -> LOG.info("Sent responses to Slack."),
            ex ->
                sendErrorToSlack(
                    ctx,
                    event,
                    """
                    Problems executing the task, you can try retrying it.
                    Detailed cause: """
                        + exceptionMessage(ex)));
  }
}
