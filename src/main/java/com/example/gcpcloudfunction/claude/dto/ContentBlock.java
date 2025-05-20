package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentBlock {
    private String type; // "text", "tool_use"
    private String text; // if type is "text"
    private String id;   // if type is "tool_use" (Claude's request)
    private String name; // if type is "tool_use" (Claude's request)
    private Map<String, Object> input; // if type is "tool_use" (Claude's request)
    @com.fasterxml.jackson.annotation.JsonProperty("tool_use_id") // For sending tool results back
    private String toolUseId; // if type is "tool_result"

    public ContentBlock() {
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() { // Corresponds to Claude's tool_use id
        return id;
    }

    public void setId(String id) { // Corresponds to Claude's tool_use id
        this.id = id;
    }

    public String getToolUseId() { // For sending tool_result
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) { // For sending tool_result
        this.toolUseId = toolUseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}
