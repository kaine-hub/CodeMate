package com.codemate.alpha.repository;

import com.codemate.alpha.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}