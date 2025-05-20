package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeResponse {
    private String id;
    private String type; // e.g., "message"
    private String role; // e.g., "assistant"
    private List<ContentBlock> content;
    private String model;
    @JsonProperty("stop_reason")
    private String stopReason; // e.g., "tool_use", "end_turn"
    @JsonProperty("stop_sequence")
    private String stopSequence;
    // Add usage if needed: private Usage usage;

    public ClaudeResponse() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public void setContent(List<ContentBlock> content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public String getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(String stopSequence) {
        this.stopSequence = stopSequence;
    }

    // If you add Usage class:
    // public Usage getUsage() { return usage; }
    // public void setUsage(Usage usage) { this.usage = usage; }

    // Inner class for usage details if needed
    // @JsonIgnoreProperties(ignoreUnknown = true)
    // public static class Usage {
    //     @JsonProperty("input_tokens")
    //     private int inputTokens;
    //     @JsonProperty("output_tokens")
    //     private int outputTokens;
    //     // Getters and setters
    // }
}
