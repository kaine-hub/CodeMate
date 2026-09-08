package com.codemate.alpha.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import com.codemate.alpha.entity.USER.Skill;
import com.codemate.alpha.repository.SkillRepository;
import com.codemate.alpha.response.SkillResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    @Autowired
    private SkillRepository skillRepository;

    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        List<SkillResponse> skills = skillRepository.findAll()
                .stream()
                .map(SkillResponse::from)
                .toList();
        return ResponseEntity.ok(skills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        return skillRepository.findById(id)
                .map(skill -> ResponseEntity.ok(SkillResponse.from(skill)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(@RequestBody Skill skill) {
        Skill saved = skillRepository.save(skill);
        return ResponseEntity.status(HttpStatus.CREATED).body(SkillResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(@PathVariable Long id, @RequestBody Skill updatedSkill) {
        return skillRepository.findById(id)
                .map(existing -> {
                    existing.setSkillName(updatedSkill.getSkillName());
                    existing.setDescription(updatedSkill.getDescription());
                    Skill saved = skillRepository.save(existing);
                    return ResponseEntity.ok(SkillResponse.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        if (!skillRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        skillRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}