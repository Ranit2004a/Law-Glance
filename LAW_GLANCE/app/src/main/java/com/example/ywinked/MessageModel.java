package com.example.ywinked;

public class MessageModel {
    private String message;
    private boolean isUser;
    private boolean isThinking;
    private String databaseId;

    public MessageModel(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
        this.isThinking = false;
        this.databaseId = null;
    }

    public MessageModel(boolean isThinking) {
        this.message = "";
        this.isUser = false;
        this.isThinking = isThinking;
        this.databaseId = null;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean isThinking() {
        return isThinking;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }
}
