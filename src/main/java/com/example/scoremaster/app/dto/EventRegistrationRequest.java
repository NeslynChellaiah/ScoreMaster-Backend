package com.example.scoremaster.app.dto;

import lombok.Data;

@Data
public class EventRegistrationRequest {
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private String location;
    private String organizer;
}