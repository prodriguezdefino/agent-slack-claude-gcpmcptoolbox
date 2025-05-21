package org.example.gcp.slack.claude.handlers;

import static org.example.gcp.slack.claude.common.Utils.parseSlackRequest;
import static org.example.gcp.slack.claude.common.Utils.processSlackRequest;

import com.slack.api.bolt.App;
import com.slack.api.bolt.util.SlackRequestParser;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** */
@Component
public class Chat {
  private final App slackApp;
  private final SlackRequestParser requestParser;

  public Chat(App slackApp, SlackRequestParser requestParser) {
    this.slackApp = slackApp;
    this.requestParser = requestParser;
  }

  public Mono<ServerResponse> handleChat(ServerRequest request) {
    return request
        .bodyToMono(String.class)
        .flatMap(
            body ->
                processSlackRequest(slackApp, parseSlackRequest(requestParser, request, body))
                    .map(
                        response ->
                            ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new SlackResponse(response.getBody())))
                    .orElse(
                        ex ->
                            ServerResponse.status(HttpStatusCode.valueOf(500))
                                .bodyValue(new SlackResponse(ex.getMessage()))))
        .switchIfEmpty(ServerResponse.badRequest().build());
  }

  record SlackResponse(String content) {}
}
