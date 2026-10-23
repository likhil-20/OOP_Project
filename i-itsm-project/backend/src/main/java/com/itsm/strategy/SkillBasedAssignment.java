package com.itsm.strategy;
import com.itsm.model.Incident;
import com.itsm.model.User;
import java.util.List;

public class SkillBasedAssignment implements AssignmentStrategy {
    @Override
    public User assignTechnician(Incident incident, List<User> technicians) {
        return technicians.stream()
            .filter(t -> t.getSkills() != null && t.getSkills().contains(incident.getCategory()))
            .findFirst()
            .orElse(technicians.isEmpty() ? null : technicians.get(0));
    }
}
