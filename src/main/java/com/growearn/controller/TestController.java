package com.growearn.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String test() {
        return "Backend is running successfully!";
    }

    @GetMapping("/api/protected")
    public String protectedApi() {
        return "You accessed a protected API!";
    }
}
