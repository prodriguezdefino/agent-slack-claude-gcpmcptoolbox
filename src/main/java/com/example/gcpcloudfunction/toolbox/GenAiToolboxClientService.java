package com.example.gcpcloudfunction.toolbox;

import com.example.gcpcloudfunction.toolbox.dto.ToolboxQueryRequest;
import com.example.gcpcloudfunction.toolbox.dto.ToolboxQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GenAiToolboxClientService {

    private static final Logger logger = LoggerFactory.getLogger(GenAiToolboxClientService.class);

    private final WebClient toolboxWebClient;

    @Autowired
    public GenAiToolboxClientService(@Qualifier("toolboxWebClient") WebClient toolboxWebClient) {
        this.toolboxWebClient = toolboxWebClient;
    }

    public Mono<ToolboxQueryResponse> executeQuery(String bigQuerySql) {
        ToolboxQueryRequest request = new ToolboxQueryRequest(bigQuerySql);

        logger.info("Executing query on GenAI Toolbox. Query: '{}'", bigQuerySql);

        return toolboxWebClient.post()
                .uri("/query-bigquery") // Assuming this is the correct endpoint on the toolbox service
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            logger.error("Error from GenAI Toolbox service: Status {}, Body {}", clientResponse.statusCode(), body);
                            // Ensure the error response DTO is populated correctly for Claude
                            ToolboxQueryResponse errorDto = new ToolboxQueryResponse();
                            errorDto.setStatus("ERROR_API_" + clientResponse.statusCode());
                            // Try to put the error message or body into a field Claude might see via formatting
                            // For example, in a 'rows' map or a dedicated error field if ClaudeService.formatToolboxResponseForClaude handles it.
                            Map<String, Object> errorDetails = new HashMap<>();
                            errorDetails.put("error_type", "API_ERROR");
                            errorDetails.put("status_code", clientResponse.statusCode().value());
                            errorDetails.put("error_message", body);
                            errorDto.setRows(List.of(errorDetails)); // Use rows to convey error details
                            return Mono.just(errorDto); // Return the DTO directly, not an error, so Claude can be informed
                        })
                        // If the response body can't be read, or for other client-side errors before sending the request.
                        .switchIfEmpty(Mono.defer(() -> {
                             logger.error("Error from GenAI Toolbox service: Status {} with empty body", clientResponse.statusCode());
                             ToolboxQueryResponse errorDto = new ToolboxQueryResponse();
                             errorDto.setStatus("ERROR_API_" + clientResponse.statusCode() + "_EMPTY_BODY");
                             Map<String, Object> errorDetails = new HashMap<>();
                             errorDetails.put("error_type", "API_ERROR_EMPTY_BODY");
                             errorDetails.put("status_code", clientResponse.statusCode().value());
                             errorDto.setRows(List.of(errorDetails));
                             return Mono.just(errorDto);
                        }))
                )
                .bodyToMono(ToolboxQueryResponse.class)
                .doOnSuccess(response -> {
                    if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                        logger.info("Successfully received response from GenAI Toolbox. Status: {}", response.getStatus());
                    } else {
                        logger.warn("Received non-SUCCESS ({}) response from GenAI Toolbox. Response: {}", response.getStatus(), response);
                    }
                })
                .doOnError(error -> { // Catch other errors like network issues or deserialization problems
                     if (!(error instanceof RuntimeException && error.getMessage().startsWith("GenAI Toolbox service Error:"))) { // Avoid double logging
                        logger.error("Error during GenAI Toolbox service call for query '{}': {}", bigQuerySql, error.getMessage(), error);
                     }
                })
                .onErrorResume(error -> {
                    logger.error("Fallback error handling for GenAI Toolbox service query '{}': {}", bigQuerySql, error.getMessage(), error);
                    // Fallback or custom error response
                    ToolboxQueryResponse errorResponse = new ToolboxQueryResponse();
                    errorResponse.setStatus("ERROR_CLIENT_SIDE: " + error.getClass().getSimpleName());
                     Map<String, Object> errorDetails = new HashMap<>();
                    errorDetails.put("error_type", "CLIENT_EXCEPTION");
                    errorDetails.put("error_message", error.getMessage());
                    errorResponse.setRows(List.of(errorDetails));
                    return Mono.just(errorResponse);
                });
    }
}
