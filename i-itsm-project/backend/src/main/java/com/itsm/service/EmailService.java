package com.itsm.service;

import com.itsm.model.EmailLog;
import com.itsm.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.itsm.exception.ForbiddenException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.model.User;
import java.util.List;

@Service
public class EmailService {

    @Autowired private EmailLogRepository emailLogRepository;

    // "Sends" an email by saving it. The in-website mailbox shows it to the person.
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) return;

        EmailLog mail = new EmailLog();
        mail.setRecipientEmail(to);
        mail.setSubject(subject);
        mail.setBody(body);
        emailLogRepository.save(mail);

        System.out.println("[EMAIL] To: " + to + " | " + subject);
    }
    
    public List<EmailLog> getFor(User me) {
        return emailLogRepository.findByRecipientEmailOrderBySentAtDesc(me.getEmail());
    }

    public List<EmailLog> getAll() {
        return emailLogRepository.findAllByOrderBySentAtDesc();
    }

    public void markOpened(Long id, User me) {
        EmailLog mail = emailLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Email not found."));
        if (!me.getEmail().equals(mail.getRecipientEmail())) {
            throw new ForbiddenException("This email is not yours.");
        }
        mail.setOpened(true);
        emailLogRepository.save(mail);
    }
}