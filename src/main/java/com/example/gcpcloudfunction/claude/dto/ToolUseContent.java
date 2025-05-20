package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolUseContent extends MessageContent {
    private String id;
    private String name;
    private Map<String, Object> input;

    public ToolUseContent() {
        super("tool_use");
    }

    public ToolUseContent(String id, String name, Map<String, Object> input) {
        super("tool_use");
        this.id = id;
        this.name = name;
        this.input = input;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
