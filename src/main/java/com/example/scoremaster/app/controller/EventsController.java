package com.example.scoremaster.app.controller;

import com.example.scoremaster.app.dto.EventRegistrationRequest;
import com.example.scoremaster.app.model.Event;
import com.example.scoremaster.app.repository.EventRepository;
import com.example.scoremaster.app.model.Role;
import com.example.scoremaster.app.model.User;
import com.example.scoremaster.app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/events")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
public class EventsController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventRegistrationRequest newEvent) {
        LocalDate startDate = LocalDate.parse(newEvent.getStartDate());
        LocalDate endDate = LocalDate.parse(newEvent.getEndDate());

        if (endDate.isBefore(startDate))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("End date cannot be before start date");

        // Generate ID: lowercase event name trimmed and with spaces replaced by
        // underscores
        String eventId = newEvent.getName().trim().toLowerCase().replaceAll("\\s+", "_");

        if (eventRepository.existsById(eventId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Event ID (derived from name) already exists");
        }

        if (userRepository.existsById(newEvent.getOrganizer())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username (Organizer) already exists");
        }

        Event event = Event.builder()
                .id(eventId)
                .name(newEvent.getName())
                .description(newEvent.getDescription())
                .location(newEvent.getLocation())
                .organizer(newEvent.getOrganizer())
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Extract suffix from organizer name (which already includes the event prefix)
        String fullOrganizer = savedEvent.getOrganizer();
        String prefix = savedEvent.getId() + "_";
        String usernameSuffix = fullOrganizer.startsWith(prefix) ? fullOrganizer.substring(prefix.length()) : fullOrganizer;

        User judge = User.builder()
                .id(fullOrganizer)
                .username(usernameSuffix)
                .password(passwordEncoder.encode(fullOrganizer))
                .role(Role.ROLE_JUDGE)
                .eventId(savedEvent.getId())
                .build();

        userRepository.save(judge);

        return ResponseEntity.ok(savedEvent);
    }

    @GetMapping
    public ResponseEntity<Page<Event>> getAllEvents(
            @RequestParam(required = false) String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(eventRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable String id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable String id,
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
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}