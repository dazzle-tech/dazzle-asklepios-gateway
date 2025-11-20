package com.dazzle.asklepios.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.senderNumber}")
    private String senderNumber;

    public void sendSms(String phone, String text) {

        Twilio.init(accountSid, authToken);

        Message.creator(
            new com.twilio.type.PhoneNumber(phone),
            new com.twilio.type.PhoneNumber(senderNumber),
            text
        ).create();
    }
}

