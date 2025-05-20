package com.example.gcpcloudfunction.claude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// This is a marker interface or common base for content types.
// For the current setup, ContentBlock itself can be used in List<ContentBlock> in Message.
// If we needed more distinct types for serialization, this could be useful.
// For now, List<ContentBlock> in Message.java is the more direct approach.
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true // Makes the 'type' property available for deserialization into subclasses if needed
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextContent.class, name = "text"),
    @JsonSubTypes.Type(value = ToolUseContent.class, name = "tool_use"),
    @JsonSubTypes.Type(value = ToolResultContent.class, name = "tool_result")
    // Add other content types if Claude API supports more in the future
})
public abstract class MessageContent {
    private String type;

    public MessageContent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
