package com.example.gcpcloudfunction.claude;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClaudeConfig {

    @Value("${claude.apiKey}")
    private String claudeApiKey;

    @Value("${claude.apiUrl}")
    private String claudeApiUrl;

    // As of March 2024, "2023-06-01" is the recommended version for tool use.
    // See: https://docs.anthropic.com/claude/reference/versions
    private static final String ANTHROPIC_VERSION = "2023-06-01"; 

    @Bean
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(claudeApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", claudeApiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }
}
