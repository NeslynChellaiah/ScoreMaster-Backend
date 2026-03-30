package com.example.scoremaster.app.controller;

import com.example.scoremaster.app.dto.EventRegistrationRequest;
import com.example.scoremaster.app.model.Event;
import com.example.scoremaster.app.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/events")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
public class EventsController {

    private final EventRepository eventRepository;

    @PostMapping
    public ResponseEntity<EventRegistrationRequest> createEvent(@RequestBody EventRegistrationRequest newEvent) {
        // start date end date validation
        Event event = Event.builder()
                .name(newEvent.getName())
                .description(newEvent.getDescription())
                .location(newEvent.getLocation())
                .organizer(newEvent.getOrganizer())
                .startDate(LocalDate.parse(newEvent.getStartDate()))
                .endDate(LocalDate.parse(newEvent.getEndDate()))
                .build();

        eventRepository.save(event);
        return ResponseEntity.ok(newEvent);
    }
}