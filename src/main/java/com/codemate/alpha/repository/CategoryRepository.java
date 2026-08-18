package com.codemate.alpha.repository;

import com.codemate.alpha.entity.PROJECT.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}