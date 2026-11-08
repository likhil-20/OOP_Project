package com.itsm.observer;

import com.itsm.model.Incident;
import com.itsm.model.User;
import com.itsm.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailObserver implements IncidentObserver {

    @Autowired private EmailService emailService;

    @Override
    public void update(IncidentEvent e) {
        Incident inc = e.incident();
        String id = "#" + inc.getId();

        if ("INCIDENT_CREATED".equals(e.type())) {
            mail(inc.getReporter(), "Incident " + id + " created",
                "Your incident " + id + " was created with priority " + inc.getPriority() + ".\n\n" + inc.getDescription());
            mail(inc.getAssignedTechnician(), "Incident " + id + " assigned to you",
                "Incident " + id + " (" + inc.getPriority() + ") was assigned to you.\n\n" + inc.getDescription());
        } else if ("STATUS_UPDATE".equals(e.type())) {
            mail(inc.getReporter(), "Incident " + id + " is now " + e.newState(),
                "The status of your incident " + id + " changed to " + e.newState() + ".");
        } else if ("INFO_UPDATE".equals(e.type())) {
            boolean actorIsReporter = inc.getReporter() != null
                && e.actorEmail().equals(inc.getReporter().getEmail());
            User target = actorIsReporter ? inc.getAssignedTechnician() : inc.getReporter();
            mail(target, "Update on incident " + id, e.description() + ".");
        }
    }

    private void mail(User user, String subject, String text) {
        if (user == null) return;
        emailService.send(user.getEmail(), "[I-ITSM] " + subject,
            "Hello " + user.getName() + ",\n\n" + text + "\n\nI-ITSM Service Desk");
    }
}