package com.itsm.observer;

import com.itsm.model.AuditLog;
import com.itsm.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditObserver implements IncidentObserver {

    @Autowired private AuditLogRepository auditRepository;

    @Override
    public void update(IncidentEvent e) {
        AuditLog a = new AuditLog();
        a.setIncidentId(e.incident().getId());
        a.setUserEmail(e.actorEmail());
        a.setActionPerformed(e.type());
        a.setPreviousState(e.previousState());
        a.setNewState(e.newState());
        a.setDescription(e.description());
        auditRepository.save(a);
    }
}
