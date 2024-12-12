package com.certapp.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/.well-known/acme-challenge")
public class ChallengeController {
    private static final Map<String, String> CHALLENGE_RESPONSES = new ConcurrentHashMap<>();
    
    @GetMapping("/{token}")
    public String getChallenge(@PathVariable String token) {
        return CHALLENGE_RESPONSES.get(token);
    }
    
    public static void addChallengeResponse(String token, String authorization) {
        CHALLENGE_RESPONSES.put(token, authorization);
    }
} 