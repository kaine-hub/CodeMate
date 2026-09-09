package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Skill;
import com.codemate.alpha.entity.User;
import com.codemate.alpha.entity.UserSkill;
import com.codemate.alpha.repository.SkillRepository;
import com.codemate.alpha.repository.UserRepository;
import com.codemate.alpha.repository.UserSkillRepository;
import com.codemate.alpha.response.UserSkillResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-skills")
public class UserSkillController {

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @GetMapping
    public ResponseEntity<List<UserSkillResponse>> getAllUserSkills() {
        List<UserSkillResponse> userSkills = userSkillRepository.findAll()
                .stream()
                .map(UserSkillResponse::from)
                .toList();
        return ResponseEntity.ok(userSkills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSkillResponse> getUserSkillById(@PathVariable Long id) {
        return userSkillRepository.findById(id)
                .map(userSkill -> ResponseEntity.ok(UserSkillResponse.from(userSkill)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSkillResponse>> getSkillsByUser(@PathVariable Long userId) {
        List<UserSkillResponse> userSkills = userSkillRepository.findByUser_UserId(userId)
                .stream()
                .map(UserSkillResponse::from)
                .toList();
        return ResponseEntity.ok(userSkills);
    }

    @GetMapping("/skill/{skillId}")
    public ResponseEntity<List<UserSkillResponse>> getUsersBySkill(@PathVariable Long skillId) {
        List<UserSkillResponse> userSkills = userSkillRepository.findBySkill_SkillId(skillId)
                .stream()
                .map(UserSkillResponse::from)
                .toList();
        return ResponseEntity.ok(userSkills);
    }

    @PostMapping
    public ResponseEntity<?> assignSkillToUser(@RequestParam Long userId,
                                                @RequestParam Long skillId,
                                                @RequestParam(required = false) String proficiencyLevel) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found with id: " + userId);
        }

        Skill skill = skillRepository.findById(skillId).orElse(null);
        if (skill == null) {
            return ResponseEntity.badRequest().body("Skill not found with id: " + skillId);
        }

        if (userSkillRepository.existsByUser_UserIdAndSkill_SkillId(userId, skillId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This skill is already assigned to the user.");
        }

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setProficiencyLevel(proficiencyLevel);

        UserSkill saved = userSkillRepository.save(userSkill);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserSkillResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSkillResponse> updateProficiency(@PathVariable Long id,
                                                                 @RequestParam String proficiencyLevel) {
        return userSkillRepository.findById(id)
                .map(existing -> {
                    existing.setProficiencyLevel(proficiencyLevel);
                    UserSkill saved = userSkillRepository.save(existing);
                    return ResponseEntity.ok(UserSkillResponse.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeUserSkill(@PathVariable Long id) {
        if (!userSkillRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userSkillRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}