package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.domain.enumeration.Gender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/enum")
public class EnumController {


    @GetMapping("/gender")
    public ResponseEntity<List<Gender>> getAllGenders() {
        List<Gender> departmentTypes = Arrays.asList(Gender.values());
        return ResponseEntity.ok(departmentTypes);
    }

}
