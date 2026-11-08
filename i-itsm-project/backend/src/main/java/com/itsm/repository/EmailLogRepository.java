package com.itsm.repository;

import com.itsm.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientEmailOrderBySentAtDesc(String recipientEmail);
    List<EmailLog> findAllByOrderBySentAtDesc();
}