package com.example.gcpcloudfunction.claude;

import com.example.gcpcloudfunction.claude.dto.ClaudeRequest;
import com.example.gcpcloudfunction.claude.dto.ClaudeResponse;
import com.example.gcpcloudfunction.claude.dto.ContentBlock;
import com.example.gcpcloudfunction.claude.dto.Message;
import com.example.gcpcloudfunction.claude.dto.TextContent;
import com.example.gcpcloudfunction.claude.dto.ToolResultContent;
import com.example.gcpcloudfunction.toolbox.dto.ToolboxQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClaudeServiceTests {

    @Mock
    private WebClient mockWebClient;
    @Mock
    private WebClient.RequestBodyUriSpec mockRequestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec mockRequestBodySpec;
    @Mock
    private WebClient.ResponseSpec mockResponseSpec;

    private ClaudeService claudeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<ClaudeRequest> claudeRequestCaptor;

    @BeforeEach
    void setUp() {
        claudeService = new ClaudeService(mockWebClient, objectMapper);

        // Common mocking sequence for WebClient
        when(mockWebClient.post()).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(anyString())).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.bodyValue(any())).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.retrieve()).thenReturn(mockResponseSpec);
    }

    private String createClaudeResponseJson(String id, String type, String role, List<ContentBlock> content, String stopReason, String model) throws JsonProcessingException {
        ClaudeResponse response = new ClaudeResponse();
        response.setId(id);
        response.setType(type);
        response.setRole(role);
        response.setContent(content);
        response.setStopReason(stopReason);
        response.setModel(model);
        return objectMapper.writeValueAsString(response);
    }

    @Test
    void testGetClaudeResponse_success_directAnswer() throws JsonProcessingException {
        // Arrange
        String userMessage = "Hello Claude";
        ContentBlock textBlock = new ContentBlock();
        textBlock.setType("text");
        textBlock.setText("Hello! How can I help you today?");
        String mockResponseJson = createClaudeResponseJson("msg_01", "message", "assistant", List.of(textBlock), "end_turn", "claude-3-opus-20240229");
        when(mockResponseSpec.bodyToMono(ClaudeResponse.class)).thenReturn(Mono.just(objectMapper.readValue(mockResponseJson, ClaudeResponse.class)));
        when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec);


        // Act
        Mono<ClaudeResponse> responseMono = claudeService.getClaudeResponse(userMessage, new ArrayList<>());

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertEquals("assistant", response.getRole());
                assertEquals("end_turn", response.getStopReason());
                assertFalse(response.getContent().isEmpty());
                assertEquals("text", response.getContent().get(0).getType());
                assertEquals("Hello! How can I help you today?", response.getContent().get(0).getText());
                return true;
            })
            .verifyComplete();

        verify(mockRequestBodySpec).bodyValue(claudeRequestCaptor.capture());
        ClaudeRequest capturedRequest = claudeRequestCaptor.getValue();
        assertEquals("claude-3-opus-20240229", capturedRequest.getModel());
        assertEquals(1, capturedRequest.getMessages().size());
        assertEquals("user", capturedRequest.getMessages().get(0).getRole());
        assertEquals(userMessage, capturedRequest.getMessages().get(0).getContent());
        assertNotNull(capturedRequest.getTools());
        assertEquals(1, capturedRequest.getTools().size());
        assertEquals("executeQueryOnBigQuery", capturedRequest.getTools().get(0).getName());
    }

    @Test
    void testGetClaudeResponse_success_toolUseRequested() throws JsonProcessingException {
        // Arrange
        String userMessage = "Query BigQuery for sales data";
        ContentBlock toolUseBlock = new ContentBlock();
        toolUseBlock.setType("tool_use");
        toolUseBlock.setId("toolu_123");
        toolUseBlock.setName("executeQueryOnBigQuery");
        toolUseBlock.setInput(Map.of("query", "SELECT * FROM sales"));
        String mockResponseJson = createClaudeResponseJson("msg_02", "message", "assistant", List.of(toolUseBlock), "tool_use", "claude-3-opus-20240229");
        when(mockResponseSpec.bodyToMono(ClaudeResponse.class)).thenReturn(Mono.just(objectMapper.readValue(mockResponseJson, ClaudeResponse.class)));
        when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec);

        // Act
        Mono<ClaudeResponse> responseMono = claudeService.getClaudeResponse(userMessage, new ArrayList<>());

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertEquals("tool_use", response.getStopReason());
                ContentBlock cb = response.getContent().get(0);
                assertEquals("tool_use", cb.getType());
                assertEquals("toolu_123", cb.getId());
                assertEquals("executeQueryOnBigQuery", cb.getName());
                assertEquals("SELECT * FROM sales", ((Map<String, Object>)cb.getInput()).get("query"));
                return true;
            })
            .verifyComplete();
         verify(mockRequestBodySpec).bodyValue(claudeRequestCaptor.capture());
        ClaudeRequest capturedRequest = claudeRequestCaptor.getValue();
        assertEquals("user", capturedRequest.getMessages().get(0).getRole());
        assertEquals(userMessage, capturedRequest.getMessages().get(0).getContent());

    }

    @Test
    void testGetClaudeResponseWithToolResults_success() throws JsonProcessingException {
        // Arrange
        List<Message> history = new ArrayList<>();
        history.add(new Message("user", "Query sales for product X"));
        // Assistant's response requesting tool use
        ContentBlock toolUseCb = new ContentBlock();
        toolUseCb.setType("tool_use");
        toolUseCb.setId("toolu_abc");
        toolUseCb.setName("executeQueryOnBigQuery");
        toolUseCb.setInput(Map.of("query", "SELECT * FROM product_x_sales"));
        Message assistantToolRequestMsg = new Message("assistant", List.of(new TextContent("Okay, I will query that."), new com.example.gcpcloudfunction.claude.dto.ToolUseContent("toolu_abc", "executeQueryOnBigQuery", Map.of("query", "SELECT * FROM product_x_sales"))));
        history.add(assistantToolRequestMsg);


        List<ToolResultContent> toolResults = List.of(
            new ToolResultContent("toolu_abc", "{\"status\":\"SUCCESS\",\"column_names\":[\"total_sales\"],\"rows\":[{\"total_sales\":5000}]}", false)
        );

        ContentBlock finalTextBlock = new ContentBlock();
        finalTextBlock.setType("text");
        finalTextBlock.setText("The total sales for product X are 5000.");
        String mockFinalResponseJson = createClaudeResponseJson("msg_03", "message", "assistant", List.of(finalTextBlock), "end_turn", "claude-3-opus-20240229");
        when(mockResponseSpec.bodyToMono(ClaudeResponse.class)).thenReturn(Mono.just(objectMapper.readValue(mockFinalResponseJson, ClaudeResponse.class)));
        when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec);

        // Act
        Mono<ClaudeResponse> responseMono = claudeService.getClaudeResponseWithToolResults(history, toolResults);

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertEquals("end_turn", response.getStopReason());
                assertEquals("The total sales for product X are 5000.", response.getContent().get(0).getText());
                return true;
            })
            .verifyComplete();

        verify(mockRequestBodySpec).bodyValue(claudeRequestCaptor.capture());
        ClaudeRequest capturedRequest = claudeRequestCaptor.getValue();
        assertNull(capturedRequest.getTools(), "Tools should not be sent in a follow-up request with tool results.");
        assertEquals(3, capturedRequest.getMessages().size()); // user, assistant_tool_request, user_tool_result
        Message toolResultMessage = capturedRequest.getMessages().get(2);
        assertEquals("user", toolResultMessage.getRole());
        assertTrue(toolResultMessage.getContent() instanceof List);
        List<ToolResultContent> resultContents = (List<ToolResultContent>) toolResultMessage.getContent();
        assertEquals(1, resultContents.size());
        assertEquals("tool_result", resultContents.get(0).getType());
        assertEquals("toolu_abc", resultContents.get(0).getToolUseId());
    }

    @Test
    void testFormatToolboxResponseForClaude_success() throws JsonProcessingException {
        ToolboxQueryResponse toolboxResponse = new ToolboxQueryResponse();
        toolboxResponse.setStatus("SUCCESS");
        toolboxResponse.setColumnNames(List.of("colA", "colB"));
        toolboxResponse.setRows(List.of(Map.of("colA", "val1", "colB", "val2")));

        String jsonOutput = claudeService.formatToolboxResponseForClaude(toolboxResponse);
        Map<String, Object> resultMap = objectMapper.readValue(jsonOutput, Map.class);

        assertEquals("SUCCESS", resultMap.get("status"));
        assertTrue(resultMap.containsKey("column_names"));
        assertTrue(resultMap.containsKey("rows"));
    }

    @Test
    void testFormatToolboxResponseForClaude_errorFromToolbox() throws JsonProcessingException {
        ToolboxQueryResponse toolboxResponse = new ToolboxQueryResponse();
        toolboxResponse.setStatus("ERROR_QUERY_FAILED");
        toolboxResponse.setRows(List.of(Map.of("error_message", "Syntax error in SQL")));


        String jsonOutput = claudeService.formatToolboxResponseForClaude(toolboxResponse);
        Map<String, Object> resultMap = objectMapper.readValue(jsonOutput, Map.class);
        
        assertEquals("ERROR_QUERY_FAILED", resultMap.get("status"));
        assertEquals("Tool execution failed, returned no data, or an error occurred.", resultMap.get("message"));
        assertEquals("Syntax error in SQL", resultMap.get("details"));
    }
    
    @Test
    void testFormatToolboxResponseForClaude_nullResponse() throws JsonProcessingException {
        String jsonOutput = claudeService.formatToolboxResponseForClaude(null);
        Map<String, Object> resultMap = objectMapper.readValue(jsonOutput, Map.class);
        assertEquals("No response from toolbox service", resultMap.get("error"));
    }


    @Test
    void testClaudeApiError() {
        // Arrange
        String userMessage = "Test API Error";
         // Simulate an API error by having onStatus trigger the error creation
        when(mockResponseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            java.util.function.Predicate<org.springframework.http.HttpStatusCode> predicate = invocation.getArgument(0);
            if (predicate.test(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)) {
                java.util.function.Function<org.springframework.web.reactive.function.client.ClientResponse, Mono<Throwable>> errorFunction = invocation.getArgument(1);
                org.springframework.web.reactive.function.client.ClientResponse mockClientResponse = mock(org.springframework.web.reactive.function.client.ClientResponse.class);
                when(mockClientResponse.statusCode()).thenReturn(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
                when(mockClientResponse.bodyToMono(String.class)).thenReturn(Mono.just("Internal Server Error Body"));
                return errorFunction.apply(mockClientResponse); // This should return the Mono<Throwable>
            }
            return mockResponseSpec; // Should not happen if predicate matches
        });
        // This ensures that when onStatus is called and the predicate matches, it executes the error function.
        // The bodyToMono call is then supposed to be chained after onStatus, but onStatus itself returns the error Mono.
        // So we make bodyToMono return an empty mono or error if it's called unexpectedly.
        when(mockResponseSpec.bodyToMono(ClaudeResponse.class)).thenReturn(Mono.error(new RuntimeException("bodyToMono should not be called directly if onStatus handles the error")));


        // Act
        Mono<ClaudeResponse> responseMono = claudeService.getClaudeResponse(userMessage, new ArrayList<>());

        // Assert
        StepVerifier.create(responseMono)
            .expectErrorMatches(throwable ->
                throwable instanceof RuntimeException &&
                throwable.getMessage().contains("Claude API Error: 500 INTERNAL_SERVER_ERROR - Internal Server Error Body")
            )
            .verify();
    }
}
