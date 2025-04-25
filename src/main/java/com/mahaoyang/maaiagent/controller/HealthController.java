package com.mahaoyang.maaiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *@Author: 叶上初阳
 *
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}
