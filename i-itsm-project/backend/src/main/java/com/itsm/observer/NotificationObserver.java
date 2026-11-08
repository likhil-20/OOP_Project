package com.itsm.observer;

import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.model.User;
import com.itsm.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

@Component
public class NotificationObserver implements IncidentObserver {

    @Autowired private NotificationRepository notificationRepository;

    @Override
    @Async
    public void update(IncidentEvent e) {
        Incident inc = e.incident();
        if ("INCIDENT_CREATED".equals(e.type())) {
            send(inc.getReporter(), "Incident #" + inc.getId() + " created (" + inc.getPriority() + ")");
            send(inc.getAssignedTechnician(), "Assigned Incident #" + inc.getId() + " (" + inc.getPriority() + ")");
        } else if ("STATUS_UPDATE".equals(e.type())) {
            send(inc.getReporter(), "Incident #" + inc.getId() + " status: " + e.newState());
        } else if ("INFO_UPDATE".equals(e.type())) {
    boolean actorIsReporter = inc.getReporter() != null
        && e.actorEmail().equals(inc.getReporter().getEmail());
    User target = actorIsReporter ? inc.getAssignedTechnician() : inc.getReporter();
    send(target, "Incident #" + inc.getId() + ": " + e.description());
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
