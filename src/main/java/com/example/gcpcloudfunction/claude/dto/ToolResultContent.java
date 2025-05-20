package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResultContent extends MessageContent {

    @JsonProperty("tool_use_id")
    private String toolUseId;
    private String content; // Can be String or structured JSON (as String)
    @JsonProperty("is_error")
    private Boolean isError; // Optional: if the tool execution resulted in an error

    public ToolResultContent() {
        super("tool_result");
    }

    public ToolResultContent(String toolUseId, String content) {
        super("tool_result");
        this.toolUseId = toolUseId;
        this.content = content;
    }

    public ToolResultContent(String toolUseId, String content, boolean isError) {
        super("tool_result");
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = isError;
    }

    // Getters and Setters
    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
}
