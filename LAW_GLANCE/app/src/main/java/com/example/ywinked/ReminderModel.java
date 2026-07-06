package com.example.ywinked;

public class ReminderModel {
    int id;
    String caseTitle;
    String courtName;
    String hearingDate;
    String hearingTime;

    public ReminderModel(int id, String caseTitle, String courtName, String hearingDate, String hearingTime) {
        this.id = id;
        this.caseTitle = caseTitle;
        this.courtName = courtName;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
    }
}
