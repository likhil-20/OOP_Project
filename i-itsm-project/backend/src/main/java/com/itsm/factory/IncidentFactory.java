package com.itsm.factory;
import com.itsm.model.Incident;
import com.itsm.model.User;

public class IncidentFactory {
    public static Incident createIncident(User reporter, String category, String description, String impact, String urgency) {
        Incident incident = new Incident();
        incident.setReporter(reporter);
        incident.setCategory(category);
        incident.setDescription(description);
        incident.setImpact(impact);
        incident.setUrgency(urgency);
        incident.setStatus("CLASSIFIED");
        return incident;
    }
}
