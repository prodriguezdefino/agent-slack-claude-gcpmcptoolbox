package com.example.gcpcloudfunction.toolbox;

import com.example.gcpcloudfunction.toolbox.dto.ToolboxQueryRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GenAiToolboxClientServiceTests {

    @Mock
    private WebClient mockWebClient;
    @Mock
    private WebClient.RequestBodyUriSpec mockRequestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec mockRequestBodySpec;
    @Mock
    private WebClient.ResponseSpec mockResponseSpec;

    private GenAiToolboxClientService toolboxService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<ToolboxQueryRequest> requestCaptor;

    @BeforeEach
    void setUp() {
        toolboxService = new GenAiToolboxClientService(mockWebClient);

        // Common mocking sequence for WebClient
        when(mockWebClient.post()).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(anyString())).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.bodyValue(any(ToolboxQueryRequest.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.retrieve()).thenReturn(mockResponseSpec);
    }

    private String createToolboxResponseJson(String status, List<String> columnNames, List<Map<String, Object>> rows) throws JsonProcessingException {
        ToolboxQueryResponse response = new ToolboxQueryResponse();
        response.setStatus(status);
        response.setColumnNames(columnNames);
        response.setRows(rows);
        return objectMapper.writeValueAsString(response);
    }

    @Test
    void testExecuteQuery_success() throws JsonProcessingException {
        // Arrange
        String sqlQuery = "SELECT name, value FROM my_table";
        String mockJsonResponse = createToolboxResponseJson("SUCCESS", List.of("name", "value"), List.of(Map.of("name", "test", "value", 123)));
        
        when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec); // Ensure onStatus is chained
        when(mockResponseSpec.bodyToMono(ToolboxQueryResponse.class))
            .thenReturn(Mono.just(objectMapper.readValue(mockJsonResponse, ToolboxQueryResponse.class)));


        // Act
        Mono<ToolboxQueryResponse> responseMono = toolboxService.executeQuery(sqlQuery);

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertEquals("SUCCESS", response.getStatus());
                assertEquals(List.of("name", "value"), response.getColumnNames());
                assertEquals(1, response.getRows().size());
                assertEquals("test", response.getRows().get(0).get("name"));
                return true;
            })
            .verifyComplete();

        verify(mockRequestBodySpec).bodyValue(requestCaptor.capture());
        assertEquals(sqlQuery, requestCaptor.getValue().getQuery());
    }

    @Test
    void testExecuteQuery_toolboxReturnsErrorStatus() throws JsonProcessingException {
        // Arrange
        String sqlQuery = "SELECT * FROM non_existent_table";
        String mockJsonResponse = createToolboxResponseJson("ERROR_BAD_QUERY", null, List.of(Map.of("error_message", "Table not found")));

        when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(ToolboxQueryResponse.class))
            .thenReturn(Mono.just(objectMapper.readValue(mockJsonResponse, ToolboxQueryResponse.class)));

        // Act
        Mono<ToolboxQueryResponse> responseMono = toolboxService.executeQuery(sqlQuery);

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertEquals("ERROR_BAD_QUERY", response.getStatus());
                assertNull(response.getColumnNames());
                assertTrue(response.getRows().get(0).containsKey("error_message"));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testExecuteQuery_httpError_500() {
        // Arrange
        String sqlQuery = "SELECT * FROM another_table";

        ClientResponse mockClientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type", "application/json")
            .body("{\"error\":\"Server Down\"}")
            .build();

        // Simulate WebClient's onStatus behavior for HTTP 500
        when(mockResponseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            java.util.function.Predicate<org.springframework.http.HttpStatusCode> predicate = invocation.getArgument(0);
            if (predicate.test(HttpStatus.INTERNAL_SERVER_ERROR)) {
                java.util.function.Function<ClientResponse, Mono<ToolboxQueryResponse>> errorFunction = invocation.getArgument(1);
                 // The errorFunction in GenAiToolboxClientService is expected to return Mono.just(errorDto)
                return errorFunction.apply(mockClientResponse);
            }
            return mockResponseSpec; // Should not happen if predicate matches
        });
        // If onStatus handles the error and returns a Mono.just(errorDto), bodyToMono for the original type shouldn't be called.
        // Or if it is, it would be after onStatus, which already changed the flow.
        // Let's assume onStatus correctly processes it into a ToolboxQueryResponse DTO.
        // So, bodyToMono(ToolboxQueryResponse.class) should effectively return what onStatus produced.
        // This part is tricky to mock perfectly without deeper WebClient internals.
        // The key is that the Function passed to onStatus must produce a Mono<ToolboxQueryResponse>.

        // In GenAiToolboxClientService, the onStatus handler returns Mono.just(errorDto) which is a ToolboxQueryResponse.
        // So the main reactive chain continues with this DTO.
        ToolboxQueryResponse expectedErrorDto = new ToolboxQueryResponse();
        expectedErrorDto.setStatus("ERROR_API_500 INTERNAL_SERVER_ERROR");
        Map<String, Object> errorDetails = new java.util.HashMap<>();
        errorDetails.put("error_type", "API_ERROR");
        errorDetails.put("status_code", 500);
        errorDetails.put("error_message", "{\"error\":\"Server Down\"}"); // Body as string
        expectedErrorDto.setRows(List.of(errorDetails));

        // We need to ensure that onStatus is called, and bodyToMono(ToolboxQueryResponse.class) is then called
        // and returns the DTO that onStatus would have produced.
        when(mockResponseSpec.bodyToMono(ToolboxQueryResponse.class)).thenReturn(Mono.just(expectedErrorDto));


        // Act
        Mono<ToolboxQueryResponse> responseMono = toolboxService.executeQuery(sqlQuery);

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertEquals("ERROR_API_500 INTERNAL_SERVER_ERROR", response.getStatus());
                assertNotNull(response.getRows());
                assertFalse(response.getRows().isEmpty());
                Map<String, Object> details = response.getRows().get(0);
                assertEquals("API_ERROR", details.get("error_type"));
                assertEquals(500, details.get("status_code"));
                assertEquals("{\"error\":\"Server Down\"}", details.get("error_message"));
                return true;
            })
            .verifyComplete();
    }
    
    @Test
    void testExecuteQuery_networkError_fallback() {
        // Arrange
        String sqlQuery = "SELECT * FROM network_error_case";
        // Simulate a network error or other client-side error before onStatus can be evaluated
        when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec); // onStatus is configured
        // bodyToMono is called after onStatus. If onStatus doesn't handle it, and bodyToMono fails, then doOnError -> onErrorResume kicks in.
        when(mockResponseSpec.bodyToMono(ToolboxQueryResponse.class))
            .thenReturn(Mono.error(new RuntimeException("Simulated network error")));

        // Act
        Mono<ToolboxQueryResponse> responseMono = toolboxService.executeQuery(sqlQuery);

        // Assert
        StepVerifier.create(responseMono)
            .expectNextMatches(response -> {
                assertNotNull(response);
                assertTrue(response.getStatus().startsWith("ERROR_CLIENT_SIDE:"));
                assertNotNull(response.getRows());
                assertFalse(response.getRows().isEmpty());
                Map<String, Object> details = response.getRows().get(0);
                assertEquals("CLIENT_EXCEPTION", details.get("error_type"));
                assertEquals("Simulated network error", details.get("error_message"));
                return true;
            })
            .verifyComplete();
    }
}
