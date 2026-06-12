package com.esprit.microservice.apicamping.dto;

import java.time.LocalDateTime;

public class NotificationDto {
    @com.fasterxml.jackson.annotation.JsonProperty("_id")
    private String id;
    
    private String recipientId;
    private String eventId;
    
    @com.fasterxml.jackson.annotation.JsonProperty("eventType")
    private String type;
    
    @com.fasterxml.jackson.annotation.JsonProperty("subject")
    private String title;
    
    // Fallback if the body is not provided
    private String message;
    private String actionUrl;
    private boolean read;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NotificationDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
