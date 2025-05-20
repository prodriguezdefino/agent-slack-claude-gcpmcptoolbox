package com.example.gcpcloudfunction.claude;

import com.example.gcpcloudfunction.claude.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ClaudeService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeService.class);

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper; // For converting tool results to JSON string

    // Consider making these configurable via application.properties
    private final String claudeModel = "claude-3-opus-20240229"; // Or claude-3-sonnet-20240229 for faster/cheaper
    private final int maxTokens = 2048;
    private final String systemPrompt = "You are a helpful assistant that can use tools to answer questions. " +
            "When appropriate, use the executeQueryOnBigQuery tool to fetch data from BigQuery. " +
            "Only use the tools provided. Only use the tool if the user is asking a question that can be answered by querying a database. " +
            "If the user is asking for a query, just provide the query directly as a text response. " +
            "If the user makes a general statement or question not suitable for BigQuery, answer it directly without using tools. " +
            "When providing results from BigQuery, present them in a readable format.";


    @Autowired
    public ClaudeService(@Qualifier("claudeWebClient") WebClient claudeWebClient, ObjectMapper objectMapper) {
        this.claudeWebClient = claudeWebClient;
        this.objectMapper = objectMapper;
    }

    public Mono<ClaudeResponse> getClaudeResponse(String userMessageText, List<Message> existingMessages) {
        ToolDefinition bigQueryTool = createBigQueryTool();
        
        List<Message> messages = new ArrayList<>(existingMessages);
        // User's current message is a simple string.
        messages.add(new Message("user", userMessageText));

        ClaudeRequest request = new ClaudeRequest(
                claudeModel,
                maxTokens,
                systemPrompt,
                messages, // Send the updated list of messages
                Collections.singletonList(bigQueryTool)
        );

        logger.info("Sending request to Claude API with model: {}, user message: '{}'", claudeModel, userMessageText);
        return claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            logger.error("Error from Claude API: Status {}, Body {}", clientResponse.statusCode(), body);
                            return Mono.error(new RuntimeException("Claude API Error: " + clientResponse.statusCode() + " - " + body));
                        })
                )
                .bodyToMono(ClaudeResponse.class)
                .doOnSuccess(response -> logger.info("Received response from Claude API. Stop reason: {}. Model: {}", response.getStopReason(), response.getModel()))
                .doOnError(error -> { // Catch other errors like network issues or deserialization problems
                    if (!(error instanceof RuntimeException && error.getMessage().startsWith("Claude API Error:"))) { // Avoid double logging
                         logger.error("Error during Claude API call for user message '{}': {}", userMessageText, error.getMessage(), error);
                    }
                });
    }

    public Mono<ClaudeResponse> getClaudeResponseWithToolResults(
        List<Message> messageHistory, // History up to and including Assistant's tool request
        List<ToolResultContent> toolResults) {

        List<Message> allMessages = new ArrayList<>(messageHistory);

        // Create the user message containing the tool results
        // This message's content will be a List<MessageContent>
        Message toolResponseMessage = new Message("user", toolResults.stream().map(tr -> (MessageContent)tr).collect(Collectors.toList()));
        allMessages.add(toolResponseMessage);

        ClaudeRequest request = new ClaudeRequest(
                claudeModel,
                maxTokens,
                systemPrompt, // System prompt might be needed for follow-up calls
                allMessages,  // Send the complete history including the new tool result message
                null          // IMPORTANT: Do not send 'tools' again on this follow-up call
        );

        logger.info("Sending follow-up request to Claude API with {} total messages, including {} tool result blocks.",
                allMessages.size(), toolResults.size());
        toolResults.forEach(tr -> logger.debug("Tool result being sent: ID {}, Content: '{}'", tr.getToolUseId(), tr.getContent()));

        return claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            logger.error("Error from Claude API on follow-up: Status {}, Body {}", clientResponse.statusCode(), body);
                            return Mono.error(new RuntimeException("Claude API Follow-up Error: " + clientResponse.statusCode() + " - " + body));
                        })
                )
                .bodyToMono(ClaudeResponse.class)
                .doOnSuccess(response -> logger.info("Received follow-up response from Claude API. Stop reason: {}. Model: {}", response.getStopReason(), response.getModel()))
                .doOnError(error -> {
                     if (!(error instanceof RuntimeException && error.getMessage().startsWith("Claude API Follow-up Error:"))) {
                        logger.error("Error during Claude API follow-up call: {}", error.getMessage(), error);
                     }
                });
    }


    private ToolDefinition createBigQueryTool() {
        Map<String, Object> inputSchemaProperties = new HashMap<>();
        inputSchemaProperties.put("query", Map.of(
                "type", "string",
                "description", "The BigQuery SQL query to execute."
        ));

        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", inputSchemaProperties,
                "required", List.of("query")
        );

        return new ToolDefinition(
                "executeQueryOnBigQuery",
                "Executes a read-only SQL query on Google BigQuery and returns the results.",
                inputSchema
        );
    }

    // Helper to convert ToolboxQueryResponse to a JSON string for Claude
    public String formatToolboxResponseForClaude(com.example.gcpcloudfunction.toolbox.dto.ToolboxQueryResponse toolboxResponse) {
        if (toolboxResponse == null) {
            return "{\"error\": \"No response from toolbox service\"}";
        }
        if (!"SUCCESS".equalsIgnoreCase(toolboxResponse.getStatus()) || toolboxResponse.getRows() == null || toolboxResponse.getColumnNames() == null) {
             // If status is not SUCCESS or data is missing, return the status or a generic error.
            String errorContent = String.format("{\"status\": \"%s\", \"message\": \"Tool execution failed or returned no data.\"}", toolboxResponse.getStatus());
            try {
                // More structured error
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("status", toolboxResponse.getStatus());
                errorMap.put("message", "Tool execution failed, returned no data, or an error occurred.");
                if (toolboxResponse.getRows() != null && toolboxResponse.getRows().size() == 1 && toolboxResponse.getRows().get(0).containsKey("error_message")){
                     errorMap.put("details", toolboxResponse.getRows().get(0).get("error_message"));
                }
                return objectMapper.writeValueAsString(errorMap);
            } catch (JsonProcessingException e) {
                logger.error("Error serializing error response for Claude: {}", e.getMessage());
                return "{\"error\": \"Failed to serialize error response for Claude.\"}";
            }
        }

        try {
            // Construct a simpler representation if needed, or just serialize the whole thing.
            // For now, serializing the relevant parts of ToolboxQueryResponse.
            Map<String, Object> result = new HashMap<>();
            result.put("status", toolboxResponse.getStatus());
            result.put("column_names", toolboxResponse.getColumnNames());
            result.put("rows", toolboxResponse.getRows());
            // Limit rows for very large results if necessary, e.g. result.put("rows", toolboxResponse.getRows().stream().limit(10).collect(Collectors.toList()));
            // And add a note like result.put("note", "Result truncated if too large...");
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing toolbox response for Claude: {}", e.getMessage());
            return "{\"error\": \"Failed to serialize toolbox query result to JSON string for Claude.\"}";
        }
    }
}
