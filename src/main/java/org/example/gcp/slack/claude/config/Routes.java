package org.example.gcp.slack.claude.config;

import org.example.gcp.slack.claude.handlers.Chat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

/** */
@Configuration
public class Routes {

  @Bean
  public RouterFunction<?> myRoutes(Chat handler) {
    return RouterFunctions.route(RequestPredicates.POST("/chat"), handler::handleChat);
  }
}
