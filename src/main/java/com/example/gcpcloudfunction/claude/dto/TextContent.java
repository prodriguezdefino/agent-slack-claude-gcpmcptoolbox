package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextContent extends MessageContent {
    private String text;

    public TextContent() {
        super("text");
    }

    public TextContent(String text) {
        super("text");
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
