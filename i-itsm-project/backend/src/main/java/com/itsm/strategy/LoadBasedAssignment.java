package com.itsm.strategy;
import com.itsm.model.Incident;
import com.itsm.model.User;
import java.util.List;
import java.util.Comparator;

public class LoadBasedAssignment implements AssignmentStrategy {
    @Override
    public User assignTechnician(Incident incident, List<User> technicians) {
        if (technicians == null || technicians.isEmpty()) return null;
        return technicians.stream()
            .min(Comparator.comparingInt(User::getCurrentLoad))
            .orElse(technicians.get(0));
    }
}
