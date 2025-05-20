package com.example.gcpcloudfunction;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.AppMentionEvent;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackAppConfig {
  private static final Logger logger = LoggerFactory.getLogger(SlackAppConfig.class);

  @Value("${slack.botToken}")
  private String botToken;

  @Value("${slack.signingSecret}")
  private String signingSecret;

  /*@Bean
  public AppConfig loadSingleWorkspaceAppConfig() {
    return AppConfig.builder().singleTeamBotToken(botToken).signingSecret(signingSecret).build();
  }

  @Bean
  public App initSlackApp(AppConfig appConfig) {
    App app = new App(appConfig);
    app.event(AppMentionEvent.class, SlackAppConfig::handleMentionEvent);
    return app;
  }*/

  static Response handleMentionEvent(EventsApiPayload<AppMentionEvent> payload, EventContext ctx) {
    AppMentionEvent event = payload.getEvent();
    String userMessageText = event.getText().replaceFirst("<@.*?>", "").trim(); // Remove mention
    String channelId = event.getChannel();
    String threadTs =
        event.getThreadTs() != null
            ? event.getThreadTs()
            : event.getTs(); // Use thread_ts or message_ts
    String historyKey = channelId + "-" + threadTs;

    ctx.logger.info(
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
                      .threadTs(
                          event.getThreadTs() != null
                              ? event.getThreadTs()
                              : event.getTs()) // Reply in thread or to original
                      // message
                      .text("some dummy message for now."));
      ctx.logger.info("Successfully sent reply to Slack. HistoryKey: {}", historyKey);
    } catch (IOException | SlackApiException e) {
      ctx.logger.error(
          "Error sending Slack reply for HistoryKey {}: {}", historyKey, e.getMessage(), e);
    }

    return ctx.ack(); // Acknowledge Slack event immediately
  }

  private void sendErrorToSlack(EventContext ctx, AppMentionEvent event, String errorMessage) {
    try {
      ctx.client()
          .chatPostMessage(
              r ->
                  r.channel(event.getChannel())
                      .threadTs(event.getThreadTs() != null ? event.getThreadTs() : event.getTs())
                      .text(errorMessage));
      logger.info("Sent error message to Slack user {}: {}", event.getUser(), errorMessage);
    } catch (IOException | SlackApiException e) {
      logger.error(
          "Failed to send error message to Slack user {}: {}", event.getUser(), e.getMessage(), e);
    }
  }
}
