package com.codemate.alpha.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class testcontroller {

    @GetMapping("test")
    public String getMethodName(@RequestParam String param) {
        return new String("Success");
    }

}
