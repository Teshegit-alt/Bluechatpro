package com.example.bluechatpro.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {
    private String messageId;
    private String content;
    private String senderName;
    private String senderAddress;
    private boolean isSent; // true if sent by current user
    private long timestamp;
    private MessageStatus status; // SENT, DELIVERED, READ
    private MessageType type; // TEXT, IMAGE, FILE

    public enum MessageStatus {
        SENDING, SENT, DELIVERED, READ, FAILED
    }

    public enum MessageType {
        TEXT, IMAGE, FILE, VOICE
    }

    // Constructor with all parameters
    public ChatMessage(String content, String senderName, String senderAddress, boolean isSent) {
        this.messageId = generateMessageId();
        this.content = content;
        this.senderName = senderName;
        this.senderAddress = senderAddress;
        this.isSent = isSent;
        this.timestamp = System.currentTimeMillis();
        this.status = isSent ? MessageStatus.SENT : MessageStatus.DELIVERED;
        this.type = MessageType.TEXT;
    }

    // Empty constructor for Firebase or serialization
    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters
    public String getMessageId() { return messageId; }
    public String getContent() { return content; }
    public String getSenderName() { return senderName; }
    public String getSenderAddress() { return senderAddress; }
    public boolean isSent() { return isSent; }
    public long getTimestamp() { return timestamp; }
    public MessageStatus getStatus() { return status; }
    public MessageType getType() { return type; }

    // Formatted time
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Setters
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setContent(String content) { this.content = content; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }
    public void setSent(boolean sent) { isSent = sent; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setStatus(MessageStatus status) { this.status = status; }
    public void setType(MessageType type) { this.type = type; }
}