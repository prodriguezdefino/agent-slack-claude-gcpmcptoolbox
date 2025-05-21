package org.example.gcp.slack.claude.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.util.SlackRequestParser;
import com.slack.api.model.event.AppMentionEvent;
import org.example.gcp.slack.claude.handlers.Mention;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackAppConfig {

  @Value("${slack.botToken}")
  private String botToken;

  @Value("${slack.signingSecret}")
  private String signingSecret;

  @Bean
  public AppConfig loadSingleWorkspaceAppConfig() {
    return AppConfig.builder().singleTeamBotToken(botToken).signingSecret(signingSecret).build();
  }

  @Bean
  public App initSlackApp(AppConfig appConfig) {
    return new App(appConfig).event(AppMentionEvent.class, Mention::handleMentionEvent);
  }

  @Bean
  public SlackRequestParser slackParse(AppConfig appConfig) {
    return new SlackRequestParser(appConfig);
  }
}
