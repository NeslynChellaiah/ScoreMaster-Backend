package com.example.scoremaster.app.config;

import com.example.scoremaster.app.model.Role;
import com.example.scoremaster.app.model.User;
import com.example.scoremaster.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsById("superadmin")) {
            User superAdmin = User.builder()
                    .id("superadmin")
                    .username("superadmin")
                    .password(passwordEncoder.encode("superadmin"))
                    .role(Role.ROLE_SUPER_ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(superAdmin);
            System.out.println("SuperAdmin created with ID: superadmin and Password: superadmin");
        }
        
        if (!userRepository.existsById("admin")) {
            User admin = User.builder()
                    .id("admin")
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ROLE_SUPER_ADMIN) // Also superadmin for convenience
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            System.out.println("Admin created with ID: admin and Password: admin");
        }
    }
}
