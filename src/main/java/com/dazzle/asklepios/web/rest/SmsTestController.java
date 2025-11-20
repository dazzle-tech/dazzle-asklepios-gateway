package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.service.SmsService;
import com.dazzle.asklepios.service.dto.SmsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class SmsTestController {

    private final SmsService smsService;

    public SmsTestController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/send-sms")
    public ResponseEntity<String> sendSms(@RequestBody SmsRequest request) {
        smsService.sendSms(request.getPhone(), request.getMessage());
        return ResponseEntity.ok("SMS sent successfully");
    }
}

