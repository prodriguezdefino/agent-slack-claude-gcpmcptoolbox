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

import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.AppMentionEvent;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** */
@Component
public class SlackSend {
  private static final Logger LOG = LoggerFactory.getLogger(SlackSend.class);

  @Async
  public CompletableFuture<Void> sendResponse(
      EventContext ctx, AppMentionEvent event, String historyKey, String textToSend) {
    try {
      ctx.client()
          .chatPostMessage(
              r ->
                  r.channel(event.getChannel())
                      .threadTs(event.getThreadTs() != null ? event.getThreadTs() : event.getTs())
                      // Reply in thread or to originalmessage
                      .text(textToSend));
      LOG.info("Successfully sent reply to Slack. HistoryKey: {}", historyKey);
      return CompletableFuture.completedFuture(null);
    } catch (IOException | SlackApiException e) {
      LOG.error("Error sending Slack reply for HistoryKey {}: {}", historyKey, e.getMessage(), e);
      return CompletableFuture.failedFuture(e);
    }
  }
}
