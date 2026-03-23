package com.example.scoremaster.app.controller;

import com.example.scoremaster.app.dto.UserRegistrationRequest;
import com.example.scoremaster.app.model.Role;
import com.example.scoremaster.app.model.User;
import com.example.scoremaster.app.repository.UserRepository;
import com.example.scoremaster.app.security.CustomUserDetails;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        Pageable pageable = PageRequest.of(page, size);

        if (isSuperAdmin) {
            if (eventId != null) {
                return ResponseEntity.ok(userRepository.findByEventId(eventId, pageable));
            } else {
                return ResponseEntity.ok(userRepository.findAll(pageable));
            }
        } else {
            Long adminEventId = null;
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                adminEventId = ((CustomUserDetails) principal).getUser().getEventId();
            }

            if (adminEventId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(userRepository.findByEventId(adminEventId, pageable));
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody UserRegistrationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        Role requestedRole = request.getRole();

        if (requestedRole == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Role must be provided");
        }

        // Only super admin can create superadmins
        if (requestedRole == Role.ROLE_SUPER_ADMIN && !isSuperAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only Super Admin can create Super Admins");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User already exists");
        }

        Long eventIdToAssign = request.getEventId();

        if (requestedRole != Role.ROLE_SUPER_ADMIN) {
            if (isSuperAdmin) {
                if (eventIdToAssign == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Event ID must be provided when Super Admin creates an Admin or Judge");
                }
            } else {
                Object principal = auth.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    eventIdToAssign = userDetails.getUser().getEventId();
                    if (eventIdToAssign == null) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("Admin does not have an associated Event ID");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Unable to determine creator's Event ID");
                }
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .eventId(eventIdToAssign)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User created successfully with role " + requestedRole);
    }
}
