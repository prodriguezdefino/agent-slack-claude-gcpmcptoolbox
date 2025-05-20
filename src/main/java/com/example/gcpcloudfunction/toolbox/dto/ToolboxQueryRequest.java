package com.example.gcpcloudfunction.toolbox.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolboxQueryRequest {
    private String query;

    public ToolboxQueryRequest() {
    }

    public ToolboxQueryRequest(String query) {
        this.query = query;
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
