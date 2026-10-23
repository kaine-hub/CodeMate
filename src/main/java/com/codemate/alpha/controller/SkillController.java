package com.codemate.alpha.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import com.codemate.alpha.entity.Skill;
import com.codemate.alpha.repository.SkillRepository;
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

    private ResponseEntity<String> bad(String m)      { return ResponseEntity.badRequest().body(m); }
    private ResponseEntity<String> conflict(String m) { return ResponseEntity.status(HttpStatus.CONFLICT).body(m); }

    @GetMapping
    public ResponseEntity<List<Skill>> getAllSkills() {
        return ResponseEntity.ok(skillRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getSkillById(@PathVariable Long id) {
        return skillRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSkill(@RequestBody Skill skill) {
        String name = skill.getSkillName() == null ? "" : skill.getSkillName().trim();
        if (name.isEmpty() || name.length() > 100) return bad("Skill name must be 1-100 characters");
        if (skillRepository.existsBySkillNameIgnoreCase(name)) return conflict("A skill with this name already exists");

        Skill toSave = new Skill();   // fresh entity so a client can't inject skillId
        toSave.setSkillName(name);
        toSave.setDescription(skill.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(skillRepository.save(toSave));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSkill(@PathVariable Long id, @RequestBody Skill updated) {
        Skill existing = skillRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        if (updated.getSkillName() != null) {
            String name = updated.getSkillName().trim();
            if (name.isEmpty() || name.length() > 100) return bad("Skill name must be 1-100 characters");
            if (skillRepository.existsBySkillNameIgnoreCaseAndSkillIdNot(name, id))
                return conflict("A skill with this name already exists");
            existing.setSkillName(name);
        }
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());

        return ResponseEntity.ok(skillRepository.save(existing));
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