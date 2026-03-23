package com.example.scoremaster.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
public class JudgeController {
    @GetMapping("/test")
    private String getUsers() {
        return "Judge Test";
    }
}
