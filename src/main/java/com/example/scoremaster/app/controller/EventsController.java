package com.example.scoremaster.app.controller;

import com.example.scoremaster.app.dto.EventRegistrationRequest;
import com.example.scoremaster.app.model.Event;
import com.example.scoremaster.app.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
        LocalDate startDate = LocalDate.parse(newEvent.getStartDate());
        LocalDate endDate = LocalDate.parse(newEvent.getEndDate());

        if (endDate.isBefore(startDate))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        Event event = Event.builder()
                .name(newEvent.getName())
                .description(newEvent.getDescription())
                .location(newEvent.getLocation())
                .organizer(newEvent.getOrganizer())
                .startDate(startDate)
                .endDate(endDate)
                .build();

        eventRepository.save(event);
        return ResponseEntity.ok(newEvent);
    }

    @GetMapping
    public ResponseEntity<Page<Event>> getAllEvents(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(eventRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id,
            @RequestBody EventRegistrationRequest updateRequest) {
        return eventRepository.findById(id).map(existingEvent -> {
            LocalDate startDate = LocalDate.parse(updateRequest.getStartDate());
            LocalDate endDate = LocalDate.parse(updateRequest.getEndDate());

            if (endDate.isBefore(startDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).<Event>build();
            }

            existingEvent.setName(updateRequest.getName());
            existingEvent.setDescription(updateRequest.getDescription());
            existingEvent.setLocation(updateRequest.getLocation());
            existingEvent.setOrganizer(updateRequest.getOrganizer());
            existingEvent.setStartDate(startDate);
            existingEvent.setEndDate(endDate);

            return ResponseEntity.ok(eventRepository.save(existingEvent));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}