package com.example.gcpcloudfunction;

package com.example.gcpcloudfunction;

import com.example.gcpcloudfunction.claude.ClaudeService;
import com.example.gcpcloudfunction.claude.dto.ContentBlock;
import com.example.gcpcloudfunction.claude.dto.Message;
import com.example.gcpcloudfunction.claude.dto.MessageContent;
import com.example.gcpcloudfunction.claude.dto.TextContent;
import com.example.gcpcloudfunction.claude.dto.ToolResultContent;
import com.example.gcpcloudfunction.claude.dto.ToolUseContent;
import com.example.gcpcloudfunction.toolbox.GenAiToolboxClientService;
import com.example.gcpcloudfunction.toolbox.dto.ToolboxQueryResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.AppMentionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Configuration
public class SlackAppConfig {
    private static final Logger logger = LoggerFactory.getLogger(SlackAppConfig.class);

    @Value("${slack.botToken}")
    private String botToken;

    @Value("${slack.signingSecret}")
    private String signingSecret;

    private final ClaudeService claudeService;
    private final GenAiToolboxClientService toolboxService;

    // Simple in-memory store for message history (for demonstration)
    // Key: channelId + "-" + threadTs (or user ID if not in thread)
    // This should be replaced with a persistent store in a production app.
    private final Map<String, List<Message>> messageHistories = new ConcurrentHashMap<>();


    @Autowired
    public SlackAppConfig(ClaudeService claudeService, GenAiToolboxClientService toolboxService) {
        this.claudeService = claudeService;
        this.toolboxService = toolboxService;
    }

    @Bean
    public AppConfig loadSingleWorkspaceAppConfig() {
        return AppConfig.builder()
                .singleTeamBotToken(botToken)
                .signingSecret(signingSecret)
                .build();
    }

    @Bean
    public App initSlackApp(AppConfig appConfig) {
        App app = new App(appConfig);

        app.event(AppMentionEvent.class, (payload, ctx) -> {
            AppMentionEvent event = payload.getEvent();
            String userMessageText = event.getText().replaceFirst("<@.*?>", "").trim(); // Remove mention
            String channelId = event.getChannel();
            String threadTs = event.getThreadTs() != null ? event.getThreadTs() : event.getTs(); // Use thread_ts or message_ts
            String historyKey = channelId + "-" + threadTs;

            ctx.logger.info("Received app_mention event from user {} in channel {}: {}", event.getUser(), channelId, userMessageText);

            // Retrieve or initialize message history
            List<Message> history = messageHistories.computeIfAbsent(historyKey, k -> new ArrayList<>());

            // Initial call to Claude
            claudeService.getClaudeResponse(userMessageText, new ArrayList<>(history)) // Pass a copy of history
                .flatMap(claudeResponse -> {
                    // Add user message and Claude's initial response to history
                    // User message was already added by ClaudeService in its internal list for the request
                    // history.add(new Message("user", userMessageText)); // Or let ClaudeService manage this
                    // Claude's response (assistant role)
                    Message assistantMessage = Message.fromClaudeResponse(claudeResponse);
                    history.add(assistantMessage); // Add assistant's response to history

                    // Check for tool use
                    List<ContentBlock> toolUseBlocks = claudeResponse.getContent() == null ? List.of() :
                        claudeResponse.getContent().stream()
                            .filter(cb -> "tool_use".equals(cb.getType()))
                            .collect(Collectors.toList());

                    if (!toolUseBlocks.isEmpty()) {
                        ctx.logger.info("Claude requested tool use. Stop Reason: {}", claudeResponse.getStopReason());
                        List<Mono<ToolResultContent>> toolResultMonos = new ArrayList<>();

                        for (ContentBlock toolUseBlock : toolUseBlocks) {
                            if ("executeQueryOnBigQuery".equals(toolUseBlock.getName())) {
                                String toolUseId = toolUseBlock.getId();
                                Map<String, Object> input = toolUseBlock.getInput();
                                String query = (String) input.get("query");

                                if (query == null || query.isBlank()) {
                                    ctx.logger.warn("Query for executeQueryOnBigQuery is null or blank for tool_use_id: {}", toolUseId);
                                    toolResultMonos.add(Mono.just(new ToolResultContent(toolUseId, "{\"error\": \"Query cannot be null or blank.\"}", true)));
                                    continue;
                                }
                                ctx.logger.info("Executing BigQuery tool with query: '{}' for tool_use_id: {}", query, toolUseId);

                                toolResultMonos.add(
                                    toolboxService.executeQuery(query)
                                        .map(toolboxResponse -> {
                                            String formattedResult = claudeService.formatToolboxResponseForClaude(toolboxResponse);
                                            boolean isError = !"SUCCESS".equalsIgnoreCase(toolboxResponse.getStatus());
                                            ctx.logger.info("Toolbox response formatted for tool_use_id {}: {}", toolUseId, formattedResult);
                                            return new ToolResultContent(toolUseId, formattedResult, isError);
                                        })
                                        .onErrorResume(e -> {
                                            ctx.logger.error("Error executing BigQuery tool for tool_use_id {}: {}", toolUseId, e.getMessage());
                                            return Mono.just(new ToolResultContent(toolUseId, "{\"error\": \"Failed to execute BigQuery query: " + e.getMessage() + "\"}", true));
                                        })
                                );
                            } else {
                                ctx.logger.warn("Unknown tool requested: {}", toolUseBlock.getName());
                                toolResultMonos.add(Mono.just(new ToolResultContent(toolUseBlock.getId(), "{\"error\": \"Tool not recognized or supported.\"}", true)));
                            }
                        }

                        return Mono.zip(toolResultMonos, toolResultObjects -> {
                                List<ToolResultContent> results = new ArrayList<>();
                                for (Object obj : toolResultObjects) {
                                    results.add((ToolResultContent) obj);
                                }
                                return results;
                            })
                            .flatMap(toolResultsList -> {
                                ctx.logger.info("Sending tool results back to Claude. Count: {}. HistoryKey: {}", toolResultsList.size(), historyKey);
                                // Message history for tool results should be up to and including the assistant's tool request.
                                return claudeService.getClaudeResponseWithToolResults(new ArrayList<>(history), toolResultsList);
                            });
                    } else {
                        // No tool use, return the initial response
                        ctx.logger.info("No tool use requested by Claude. Stop Reason: {}. HistoryKey: {}", claudeResponse.getStopReason(), historyKey);
                        return Mono.just(claudeResponse);
                    }
                })
                .flatMap(finalClaudeResponse -> {
                    // Add Claude's final response to history
                    Message finalAssistantMessage = Message.fromClaudeResponse(finalClaudeResponse);
                    history.add(finalAssistantMessage);
                    // Cap history size if needed
                    if (history.size() > 20) { // Example: keep last 20 messages
                        messageHistories.put(historyKey, new ArrayList<>(history.subList(history.size() - 20, history.size())));
                    } else {
                         messageHistories.put(historyKey, history);
                    }

                    // Extract text and send to Slack
                    String replyText = extractTextFromClaudeResponse(finalClaudeResponse);
                    if (replyText.isEmpty()) {
                        replyText = "I received a response, but it had no text content. I'll report this occurrence.";
                        ctx.logger.warn("Final Claude response had no text content. Response: {}. HistoryKey: {}", finalClaudeResponse, historyKey);
                    }
                    try {
                        // Use event.getChannel() and threadTs for consistent replies
                        ctx.client().chatPostMessage(r -> r
                            .channel(event.getChannel())
                            .threadTs(event.getThreadTs() != null ? event.getThreadTs() : event.getTs()) // Reply in thread or to original message
                            .text(replyText)
                        );
                        ctx.logger.info("Successfully sent reply to Slack. HistoryKey: {}", historyKey);
                        return Mono.empty(); // Indicate success
                    } catch (IOException | SlackApiException e) {
                        ctx.logger.error("Error sending Slack reply for HistoryKey {}: {}", historyKey, e.getMessage(), e);
                        return Mono.error(e); // Propagate error to be caught by the top-level handler
                    }
                })
                .onErrorResume(throwable -> {
                    // This is the top-level error handler for the reactive chain
                    ctx.logger.error("Critical error in Slack event processing for HistoryKey {}: {}", historyKey, throwable.getMessage(), throwable);
                    sendErrorToSlack(ctx, event, "Sorry, I encountered a critical error processing your request. Please try again later or contact support if the issue persists. Error ID: " + historyKey);
                    return Mono.empty(); // Consume the error and complete the reactive chain
                })
                .subscribe(
                    null, // onNext: not needed as actions are side effects
                    throwable -> { // onError: This will be called if onErrorResume wasn't used or re-threw.
                        // This block should ideally not be reached if onErrorResume handles everything.
                        // Logging here is a fallback.
                        ctx.logger.error("Unhandled error reached end of subscribe block for HistoryKey {}: {}", historyKey, throwable.getMessage(), throwable);
                        // Attempt to notify user one last time, though might not be possible if context is invalid
                        sendErrorToSlack(ctx, event, "An unexpected error occurred. Error ID: " + historyKey);
                    },
                    () -> ctx.logger.info("Slack event processing completed for HistoryKey: {}", historyKey) // onComplete
                );

            return ctx.ack(); // Acknowledge Slack event immediately
        });

        return app;
    }
    
    private void sendErrorToSlack(EventContext ctx, AppMentionEvent event, String errorMessage) {
        try {
            ctx.client().chatPostMessage(r -> r
                .channel(event.getChannel())
                .threadTs(event.getThreadTs() != null ? event.getThreadTs() : event.getTs())
                .text(errorMessage)
            );
            logger.info("Sent error message to Slack user {}: {}", event.getUser(), errorMessage);
        } catch (IOException | SlackApiException e) {
            logger.error("Failed to send error message to Slack user {}: {}", event.getUser(), e.getMessage(), e);
        }
    }

    private String extractTextFromClaudeResponse(com.example.gcpcloudfunction.claude.dto.ClaudeResponse claudeResponse) {
        if (claudeResponse == null || claudeResponse.getContent() == null) {
            logger.warn("Claude response or its content is null during text extraction.");
            return "";
        }
        String extractedText = claudeResponse.getContent().stream()
            .filter(cb -> "text".equals(cb.getType()) && cb.getText() != null)
            .map(ContentBlock::getText)
            .collect(Collectors.joining("\n"));
        if (extractedText.isEmpty()) {
            logger.warn("No text found in Claude response content blocks. Full response content: {}", claudeResponse.getContent());
        }
        return extractedText;
    }
}
