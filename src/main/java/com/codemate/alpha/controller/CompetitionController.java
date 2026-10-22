package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Category;
import com.codemate.alpha.entity.Competition;
import com.codemate.alpha.repository.CategoryRepository;
import com.codemate.alpha.repository.CompetitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    private static final Set<String> COMPETITION_STATUSES = Set.of("UPCOMING", "ONGOING", "COMPLETED"); // <- put your
                                                                                                        // real values
                                                                                                        // here

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private ResponseEntity<String> bad(String m) {
        return ResponseEntity.badRequest().body(m);
    }

    private ResponseEntity<String> conflict(String m) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(m);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank())
            return false;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && host != null && host.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    // call only after isValidHttpUrl passed.
    // lowercases scheme+host, drops trailing slash and #fragment so formatting
    // can't dodge duplicate checks
    private static String normalizeUrl(String value) {
        URI u = URI.create(value.trim());
        String path = u.getRawPath() == null ? "" : u.getRawPath();
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        String out = u.getScheme().toLowerCase() + "://" + u.getRawAuthority().toLowerCase() + path;
        if (u.getRawQuery() != null)
            out += "?" + u.getRawQuery();
        return out;
    }

    private String checkDatesAndLimit(LocalDate reg, LocalDate start, LocalDate end, Short limit) {
        if (start != null && end != null && end.isBefore(start))
            return "End date cannot be before start date";
        if (reg != null && start != null && reg.isAfter(start))
            return "Registration deadline must be on or before the start date";
        if (limit != null && limit < 1)
            return "teamSizeLimit must be at least 1";
        return null;
    }

    private String checkLengths(String organizer, String location) {
        if (organizer != null && organizer.length() > 150)
            return "organizer must be at most 150 characters";
        if (location != null && location.length() > 150)
            return "location must be at most 150 characters";
        return null;
    }

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

        // a competition must belong to exactly one category
        if (competition.getCategory() == null || competition.getCategory().getCategoryId() == null) {
            return bad("categoryId is required");
        }
        Category category = categoryRepository.findById(competition.getCategory().getCategoryId()).orElse(null);
        if (category == null) {
            return bad("Category not found with id: " + competition.getCategory().getCategoryId());
        }

        String name = competition.getCompetitionName() == null ? "" : competition.getCompetitionName().trim();
        if (name.isEmpty() || name.length() > 150)
            return bad("Competition name must be 1-150 characters");

        // website: valid, normalized, unique
        String website = null;
        if (!isBlank(competition.getWebsite())) {
            if (!isValidHttpUrl(competition.getWebsite()))
                return bad("website must be a valid http(s) URL");
            website = normalizeUrl(competition.getWebsite());
            if (website.length() > 150)
                return bad("website must be at most 150 characters");
            if (competitionRepository.existsByWebsite(website))
                return conflict("This website link is already used by another competition.");
        }

        String status = null;
        if (!isBlank(competition.getCompetitionStatus())) {
            status = competition.getCompetitionStatus().trim().toUpperCase();
            if (!COMPETITION_STATUSES.contains(status))
                return bad("Invalid competition status. Allowed: " + COMPETITION_STATUSES);
        }

        String problem = checkDatesAndLimit(competition.getRegistrationDeadline(), competition.getStartDate(),
                competition.getEndDate(), competition.getTeamSizeLimit());
        if (problem == null)
            problem = checkLengths(competition.getOrganizer(), competition.getLocation());
        if (problem != null)
            return bad(problem);

        competition.setCompetitionId(null); // never let POST overwrite an existing row
        competition.setCategory(category);
        competition.setCompetitionName(name);
        competition.setWebsite(website);
        competition.setCompetitionStatus(status);

        return ResponseEntity.status(HttpStatus.CREATED).body(competitionRepository.save(competition));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompetition(@PathVariable Long id, @RequestBody Competition updated) {

        Competition existing = competitionRepository.findById(id).orElse(null);
        if (existing == null)
            return ResponseEntity.notFound().build();

        Category category = existing.getCategory();
        if (updated.getCategory() != null && updated.getCategory().getCategoryId() != null) {
            category = categoryRepository.findById(updated.getCategory().getCategoryId()).orElse(null);
            if (category == null)
                return bad("Category not found with id: " + updated.getCategory().getCategoryId());
        }

        String name = existing.getCompetitionName();
        if (updated.getCompetitionName() != null) {
            name = updated.getCompetitionName().trim();
            if (name.isEmpty() || name.length() > 150)
                return bad("Competition name must be 1-150 characters");
        }

        // website: only touched when a non-blank one is sent
        String website = existing.getWebsite();
        if (!isBlank(updated.getWebsite())) {
            if (!isValidHttpUrl(updated.getWebsite()))
                return bad("website must be a valid http(s) URL");
            website = normalizeUrl(updated.getWebsite());
            if (website.length() > 150)
                return bad("website must be at most 150 characters");
            if (competitionRepository.existsByWebsiteAndCompetitionIdNot(website, id))
                return conflict("This website link is already used by another competition.");
        }

        // dates / limit validated on the effective (new-or-existing) values
        LocalDate reg = updated.getRegistrationDeadline() != null ? updated.getRegistrationDeadline()
                : existing.getRegistrationDeadline();
        LocalDate start = updated.getStartDate() != null ? updated.getStartDate() : existing.getStartDate();
        LocalDate end = updated.getEndDate() != null ? updated.getEndDate() : existing.getEndDate();
        Short limit = updated.getTeamSizeLimit() != null ? updated.getTeamSizeLimit() : existing.getTeamSizeLimit();

        String problem = checkDatesAndLimit(reg, start, end, limit);
        if (problem == null)
            problem = checkLengths(updated.getOrganizer(), updated.getLocation());
        if (problem != null)
            return bad(problem);

        String status = existing.getCompetitionStatus();
        if (updated.getCompetitionStatus() != null) {
            status = updated.getCompetitionStatus().trim().toUpperCase();
            if (!COMPETITION_STATUSES.contains(status))
                return bad("Invalid competition status. Allowed: " + COMPETITION_STATUSES);
        }

        // everything passed, now apply
        existing.setCategory(category);
        existing.setCompetitionName(name);
        existing.setWebsite(website);
        existing.setRegistrationDeadline(reg);
        existing.setStartDate(start);
        existing.setEndDate(end);
        existing.setTeamSizeLimit(limit);
        existing.setCompetitionStatus(status);
        if (updated.getOrganizer() != null)
            existing.setOrganizer(updated.getOrganizer());
        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());
        if (updated.getLocation() != null)
            existing.setLocation(updated.getLocation());

        return ResponseEntity.ok(competitionRepository.save(existing));
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