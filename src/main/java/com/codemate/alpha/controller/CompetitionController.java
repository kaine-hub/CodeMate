package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Category;
import com.codemate.alpha.entity.Competition;
import com.codemate.alpha.repository.CategoryRepository;
import com.codemate.alpha.repository.CompetitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<Competition>> getAllCompetitions() {
        return ResponseEntity.ok(competitionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Competition> getCompetitionById(@PathVariable Long id) {
        return competitionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Competition>> getCompetitionsByCategory(@PathVariable Long categoryId) {
        List<Competition> competitions = competitionRepository.findAll()
                .stream()
                .filter(c -> c.getCategory() != null && categoryId.equals(c.getCategory().getCategoryId()))
                .toList();
        return ResponseEntity.ok(competitions);
    }

    @PostMapping
    public ResponseEntity<?> createCompetition(@RequestBody Competition competition) {
        // a competition must belong to exactly one category (enforced by the single
        // @ManyToOne "category" field on the entity itself, so nothing extra to check there)
        if (competition.getCategory() == null || competition.getCategory().getCategoryId() == null) {
            return ResponseEntity.badRequest().body("categoryId is required");
        }
        Category category = categoryRepository.findById(competition.getCategory().getCategoryId()).orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest()
                    .body("Category not found with id: " + competition.getCategory().getCategoryId());
        }

        // website is not unique at the DB level, so enforce it here
        if (competition.getWebsite() != null && !competition.getWebsite().isBlank()
                && competitionRepository.existsByWebsite(competition.getWebsite())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This website link is already used by another competition.");
        }

        competition.setCategory(category);
        Competition saved = competitionRepository.save(competition);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompetition(@PathVariable Long id, @RequestBody Competition updatedCompetition) {
        return competitionRepository.findById(id)
                .map(existing -> {
                    if (updatedCompetition.getCategory() != null && updatedCompetition.getCategory().getCategoryId() != null) {
                        Category category = categoryRepository.findById(updatedCompetition.getCategory().getCategoryId())
                                .orElse(null);
                        if (category == null) {
                            return ResponseEntity.badRequest()
                                    .body("Category not found with id: " + updatedCompetition.getCategory().getCategoryId());
                        }
                        existing.setCategory(category);
                    }

                    String newWebsite = updatedCompetition.getWebsite();
                    if (newWebsite != null && !newWebsite.isBlank() && !newWebsite.equals(existing.getWebsite())) {
                        boolean takenBySomeoneElse = competitionRepository.findByWebsite(newWebsite)
                                .filter(other -> !other.getCompetitionId().equals(id))
                                .isPresent();
                        if (takenBySomeoneElse) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body("This website link is already used by another competition.");
                        }
                        existing.setWebsite(newWebsite);
                    }

                    existing.setCompetitionName(updatedCompetition.getCompetitionName());
                    existing.setOrganizer(updatedCompetition.getOrganizer());
                    existing.setDescription(updatedCompetition.getDescription());
                    existing.setLocation(updatedCompetition.getLocation());
                    existing.setRegistrationDeadline(updatedCompetition.getRegistrationDeadline());
                    existing.setStartDate(updatedCompetition.getStartDate());
                    existing.setEndDate(updatedCompetition.getEndDate());
                    existing.setTeamSizeLimit(updatedCompetition.getTeamSizeLimit());
                    existing.setCompetitionStatus(updatedCompetition.getCompetitionStatus());

                    Competition saved = competitionRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        if (!competitionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        competitionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
