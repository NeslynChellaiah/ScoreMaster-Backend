package com.example.scoremaster.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/test")
    public String getUsers() {
        return "Admin Test";
    }
}
