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
            @RequestParam(required = false) String eventId,
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
            String adminEventId = null;
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

        String eventIdToAssign = request.getEventId();

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

        String combinedId = (eventIdToAssign != null ? eventIdToAssign + "_" : "") + request.getUsername();

        if (userRepository.existsById(combinedId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User ID (derived from event and username) already exists");
        }

        User user = User.builder()
                .id(combinedId)
                .username(request.getUsername()) // Store only the suffix
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .eventId(eventIdToAssign)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User created successfully with role " + requestedRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserRegistrationRequest updateRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUserDetails = (CustomUserDetails) auth.getPrincipal();
        User currentUser = currentUserDetails.getUser();

        return userRepository.findById(id).map(targetUser -> {
            boolean isSuperAdmin = currentUser.getRole() == Role.ROLE_SUPER_ADMIN;
            boolean isSelf = currentUser.getId().equals(id);
            boolean isAdminOfSameEvent = currentUser.getRole() == Role.ROLE_ADMIN &&
                    currentUser.getEventId() != null &&
                    currentUser.getEventId().equals(targetUser.getEventId());

            // Authorization for username update
            if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(targetUser.getUsername())) {
                if (isSuperAdmin || isSelf || isAdminOfSameEvent) {
                    targetUser.setUsername(updateRequest.getUsername());
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to change this user's name");
                }
            }

            // Authorization for password update
            if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
                if (isSelf) {
                    targetUser.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the user themselves can change their password");
                }
            }

            return ResponseEntity.ok(userRepository.save(targetUser));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        return userRepository.findById(id).map(user -> {
            if (!isSuperAdmin) {
                Object principal = auth.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    String adminEventId = ((CustomUserDetails) principal).getUser().getEventId();
                    if (adminEventId == null || !adminEventId.equals(user.getEventId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                }
            }
            userRepository.delete(user);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
