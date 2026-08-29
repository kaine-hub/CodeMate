package com.codemate.alpha.repository;

import com.codemate.alpha.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}