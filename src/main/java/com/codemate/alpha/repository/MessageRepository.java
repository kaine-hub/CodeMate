package com.codemate.alpha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codemate.alpha.entity.APPLY.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}