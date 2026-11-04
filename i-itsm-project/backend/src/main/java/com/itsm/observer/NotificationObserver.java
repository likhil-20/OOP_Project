package com.itsm.observer;

import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.model.User;
import com.itsm.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationObserver implements IncidentObserver {

    @Autowired private NotificationRepository notificationRepository;

    @Override
    public void update(IncidentEvent e) {
        Incident inc = e.incident();
        if ("INCIDENT_CREATED".equals(e.type())) {
            send(inc.getReporter(), "Incident #" + inc.getId() + " created (" + inc.getPriority() + ")");
            send(inc.getAssignedTechnician(), "Assigned Incident #" + inc.getId() + " (" + inc.getPriority() + ")");
        } else if ("STATUS_UPDATE".equals(e.type())) {
            send(inc.getReporter(), "Incident #" + inc.getId() + " status: " + e.newState());
        }
    }

    private void send(User user, String message) {
        if (user == null) return;
        Notification n = new Notification();
        n.setRecipientEmail(user.getEmail());
        n.setMessage(message);
        notificationRepository.save(n);
    }
}
