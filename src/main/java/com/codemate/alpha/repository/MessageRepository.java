package com.codemate.alpha.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codemate.alpha.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByApplication_ApplicationIdOrderBySentAtAsc(Long applicationId);
}