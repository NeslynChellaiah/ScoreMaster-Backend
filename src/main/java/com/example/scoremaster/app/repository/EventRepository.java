package com.example.scoremaster.app.repository;

import com.example.scoremaster.app.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {
    Optional<Event> findByName(String name);
}
