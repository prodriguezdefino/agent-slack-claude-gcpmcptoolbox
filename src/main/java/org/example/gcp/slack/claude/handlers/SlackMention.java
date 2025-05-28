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
import static org.example.gcp.slack.claude.common.Utils.toText;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AppMentionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** */
@Component
public class SlackMention {
  private static final Logger LOG = LoggerFactory.getLogger(SlackMention.class);

  private final ClaudeChat claude;
  private final SlackOperations slack;

  public SlackMention(ClaudeChat claude, SlackOperations send) {
    this.claude = claude;
    this.slack = send;
  }

  public Response handleMentionEvent(EventsApiPayload<AppMentionEvent> payload, EventContext ctx) {
    var event = payload.getEvent();
    var userMessageText = removeMention(event.getText());
    var channelId = event.getChannel();
    var threadTs = threadTs(event);

    LOG.info(
        "Received app_mention event from user {} in channel {}: {}",
        event.getUser(),
        channelId,
        userMessageText);

    slack
        .reply(ctx, event, "Thinking...")
        .flatMap(__ -> slack.history(ctx, channelId, threadTs))
        .flatMap(previousMessages -> claude.generate(userMessageText, previousMessages))
        .flatMap(llmResponse -> slack.reply(ctx, event, toText(llmResponse)))
        .subscribe(
            __ -> LOG.info("Sent responses to Slack."),
            ex ->
                sendErrorToSlack(
                    ctx,
                    event,
                    """
                    Problems executing the task, you can retry it.
                    Detailed cause: """
                        + ex.getMessage()));

    return ctx.ack(); // Acknowledge Slack event immediately
  }
}
