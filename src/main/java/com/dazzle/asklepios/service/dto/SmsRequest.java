package com.dazzle.asklepios.service.dto;

public class SmsRequest {
    private String phone;
    private String message;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
