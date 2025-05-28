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
import static org.example.gcp.slack.claude.common.Utils.threadTs;
import static org.example.gcp.slack.claude.common.Utils.toMessage;

import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.AppMentionEvent;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** */
@Component
public class SlackOperations {
  private static final Logger LOG = LoggerFactory.getLogger(SlackOperations.class);

  public Mono<Boolean> reply(EventContext ctx, AppMentionEvent event, String textToSend) {
    return Mono.fromCallable(
            () -> {
              try {
                var post =
                    ctx.client()
                        .chatPostMessage(
                            r ->
                                r.channel(event.getChannel())
                                    .threadTs(threadTs(event))
                                    .text(textToSend));
                if (!post.isOk()) {
                  throw new RuntimeException("Problems posting a reply: " + post.getError());
                }
                LOG.info("Successfully sent reply to Slack.");
                return true;
              } catch (IOException | SlackApiException e) {
                var msg = String.format("Error sending Slack response: %s", e.getMessage());
                LOG.error(msg, e);
                throw new RuntimeException(msg, e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<List<Message>> history(EventContext ctx, String channelId, String threadId) {
    return Mono.fromCallable(
            () -> {
              try {
                var history =
                    ctx.client()
                        .conversationsReplies(
                            cr ->
                                cr.channel(channelId)
                                    .ts(threadId)
                                    .token(ctx.getBotToken())
                                    .limit(10));
                if (history.isOk()) {
                  return history.getMessages().stream()
                      .map(
                          msg ->
                              toMessage(
                                  msg.getUser(), ctx.getBotUserId(), removeMention(msg.getText())))
                      .distinct()
                      .toList();
                }
                throw new RuntimeException("Error retrieving history: " + history.getError());
              } catch (SlackApiException | IOException ex) {
                throw new RuntimeException("Error retrieving history: ", ex);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }
}
