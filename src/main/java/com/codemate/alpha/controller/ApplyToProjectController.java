package com.codemate.alpha.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemate.alpha.entity.ApplyToProject;
import com.codemate.alpha.repository.ApplyToProjectRepository;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class ApplyToProjectController {

    private final ApplyToProjectRepository repository;

    public ApplyToProjectController(ApplyToProjectRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/apply")
    public List<ApplyToProject> getAllApplications() {
        return repository.findAll();
    }

    @PostMapping("/apply")
    public ApplyToProject createApplication(@RequestBody ApplyToProject application) {
        return repository.save(application);
    }

}