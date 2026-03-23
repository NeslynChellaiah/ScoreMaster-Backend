package com.example.scoremaster.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    @GetMapping("/test")
    public String getUsers() {
        return "Super Admin Test";
    }
}
