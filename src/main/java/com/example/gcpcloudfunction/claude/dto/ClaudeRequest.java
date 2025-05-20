package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaudeRequest {
    private String model;
    @JsonProperty("max_tokens")
    private int maxTokens;
    private String system;
    private List<Message> messages;
    private List<ToolDefinition> tools;

    public ClaudeRequest() {
    }

    public ClaudeRequest(String model, int maxTokens, String system, List<Message> messages, List<ToolDefinition> tools) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.system = system;
        this.messages = messages;
        this.tools = tools;
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }
}
