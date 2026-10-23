package com.codemate.alpha.controller;

import com.codemate.alpha.entity.Category;
import com.codemate.alpha.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    private ResponseEntity<String> bad(String m)      { return ResponseEntity.badRequest().body(m); }
    private ResponseEntity<String> conflict(String m) { return ResponseEntity.status(HttpStatus.CONFLICT).body(m); }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        String name = category.getCategoryName() == null ? "" : category.getCategoryName().trim();
        if (name.isEmpty() || name.length() > 100) return bad("Category name must be 1-100 characters");
        if (categoryRepository.existsByCategoryNameIgnoreCase(name))
            return conflict("A category with this name already exists");

        Category toSave = new Category();   // fresh entity so a client can't inject categoryId
        toSave.setCategoryName(name);
        toSave.setDescription(category.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(toSave));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category updated) {
        Category existing = categoryRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        if (updated.getCategoryName() != null) {
            String name = updated.getCategoryName().trim();
            if (name.isEmpty() || name.length() > 100) return bad("Category name must be 1-100 characters");
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(name, id))
                return conflict("A category with this name already exists");
            existing.setCategoryName(name);
        }
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());

        return ResponseEntity.ok(categoryRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}