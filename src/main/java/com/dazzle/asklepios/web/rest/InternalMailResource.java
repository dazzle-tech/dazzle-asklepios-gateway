package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.service.MailService;
import com.dazzle.asklepios.service.dto.PatientCreatePasswordMailDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/internal/mail")
public class InternalMailResource {

    private final MailService mailService;

    public InternalMailResource(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/patient-create-password")
    public ResponseEntity<Void> sendPatientCreatePasswordMail(
        @RequestBody PatientCreatePasswordMailDTO dto
    ) {
        mailService.sendOneTimeSetPasswordLinkMailForPatient(
            dto.language(),
            dto.patientId(),
            dto.name(),
            dto.documentId(),
            dto.email(),
            dto.token()
        );

        return ResponseEntity.ok().build();
    }
}
