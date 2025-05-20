package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role;
    // Content can be a simple string (for text-only messages from the user)
    // or a list of MessageContent objects (for assistant messages or user messages with tool results).
    // Jackson will serialize appropriately based on the actual type of 'content'.
    private Object content;

    public Message() {
    }

    // Constructor for simple text content (typically from user)
    public Message(String role, String textContent) {
        this.role = role;
        this.content = textContent; // Keep as String for simple text
    }

    // Constructor for complex content (list of MessageContent blocks)
    public Message(String role, List<MessageContent> contentBlocks) {
        this.role = role;
        this.content = contentBlocks;
    }

    // Getters and Setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    // Setter for simple text content
    public void setContent(String textContent) {
        this.content = textContent;
    }

    // Setter for list of MessageContent blocks
    public void setContent(List<MessageContent> contentBlocks) {
        this.content = contentBlocks;
    }

    // Helper method to construct a simple text message (user or assistant)
    public static Message text(String role, String text) {
        Message msg = new Message();
        msg.setRole(role);
        // For text-only messages, Claude API might expect content to be a plain string,
        // not a list containing a single text content block.
        // However, using List<MessageContent> consistently might be more robust.
        // Let's assume for now that sending a single TextContent block in a list is fine.
        // msg.setContent(List.of(new TextContent(text)));
        // According to Anthropic docs, user messages can be string or array of blocks.
        // Assistant messages are array of blocks.
        // For simplicity in ClaudeService initial message, user content is String.
        msg.setContent(text); // Set as plain string for user's first message
        return msg;
    }

    public static Message fromClaudeResponse(com.example.gcpcloudfunction.claude.dto.ClaudeResponse claudeResponse) {
        Message assistantMessage = new Message();
        assistantMessage.setRole(claudeResponse.getRole());
        // ClaudeResponse.content is List<ContentBlock>. Need to convert to List<MessageContent>.
        // This requires ContentBlock to be adaptable or convertible to MessageContent types.
        // Given our new DTO structure, ClaudeResponse.content should be List<MessageContent>
        // or we map it here. Let's assume ClaudeResponse.content is List<ContentBlock> for now.
        
        List<MessageContent> messageContents = new java.util.ArrayList<>();
        if (claudeResponse.getContent() != null) {
            for (ContentBlock cb : claudeResponse.getContent()) {
                if ("text".equals(cb.getType())) {
                    messageContents.add(new TextContent(cb.getText()));
                } else if ("tool_use".equals(cb.getType())) {
                    messageContents.add(new ToolUseContent(cb.getId(), cb.getName(), cb.getInput()));
                }
                // tool_result type is usually not sent from assistant, but from user.
            }
        }
        assistantMessage.setContent(messageContents);
        return assistantMessage;
    }
}
