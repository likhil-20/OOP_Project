package com.itsm.service;

import com.itsm.exception.ForbiddenException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.model.Notification;
import com.itsm.model.User;
import com.itsm.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;

    public List<Notification> getFor(User me) {
        return notificationRepository.findByRecipientEmail(me.getEmail());
    }

    public void markRead(Long id, User me) {
        Notification n = notificationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (!me.getEmail().equals(n.getRecipientEmail())) {
            throw new ForbiddenException("This notification is not yours.");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }
}