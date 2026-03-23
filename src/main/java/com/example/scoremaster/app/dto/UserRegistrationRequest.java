package com.example.scoremaster.app.dto;

import com.example.scoremaster.app.model.Role;
import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String username;
    private String password;
    private Role role;
    private Long eventId;
}
