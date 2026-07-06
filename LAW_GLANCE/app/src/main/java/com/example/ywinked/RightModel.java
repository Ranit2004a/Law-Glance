package com.example.ywinked;

public class RightModel {
    String category;
    String title;
    String description;
    String cause;
    String punishment;
    boolean isExpanded = false;

    public RightModel(String category, String title, String description, String cause, String punishment) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.cause = cause;
        this.punishment = punishment;
    }
}
