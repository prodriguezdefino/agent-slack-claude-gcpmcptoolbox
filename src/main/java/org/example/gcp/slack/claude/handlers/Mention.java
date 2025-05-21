package org.example.gcp.slack.claude.handlers;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.AppMentionEvent;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class Mention {
  private static final Logger LOG = LoggerFactory.getLogger(Mention.class);

  public static Response handleMentionEvent(
      EventsApiPayload<AppMentionEvent> payload, EventContext ctx) {
    AppMentionEvent event = payload.getEvent();
    String userMessageText = event.getText().replaceFirst("<@.*?>", "").trim(); // Remove mention
    String channelId = event.getChannel();
    String threadTs =
        event.getThreadTs() != null
            ? event.getThreadTs()
            : event.getTs(); // Use thread_ts or message_ts
    String historyKey = channelId + "-" + threadTs;

    LOG.info(
        "Received app_mention event from user {} in channel {}: {}",
        event.getUser(),
        channelId,
        userMessageText);

    try {
      // Use event.getChannel() and threadTs for consistent replies
      ctx.client()
          .chatPostMessage(
              r ->
                  r.channel(event.getChannel())
                      .threadTs(event.getThreadTs() != null ? event.getThreadTs() : event.getTs())
                      // Reply in thread or to originalmessage
                      .text("some dummy message for now."));
      LOG.info("Successfully sent reply to Slack. HistoryKey: {}", historyKey);
    } catch (IOException | SlackApiException e) {
      LOG.error("Error sending Slack reply for HistoryKey {}: {}", historyKey, e.getMessage(), e);
    }

    return ctx.ack(); // Acknowledge Slack event immediately
  }
}
