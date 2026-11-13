package com.itsm.strategy;
import com.itsm.model.Incident;
import com.itsm.model.User;
import java.util.Comparator;
import java.util.List;

public class SkillBasedAssignment implements AssignmentStrategy {
    @Override
    public User assignTechnician(Incident incident, List<User> technicians) {
        if (technicians == null || technicians.isEmpty()) return null;

        Comparator<User> leastBusy = Comparator.comparingInt(User::getCurrentLoad);

        // 1) Technicians who really have the skill: pick the least busy one
        // 2) If nobody has the skill: pick the least busy technician overall
        return technicians.stream()
            .filter(t -> hasSkill(t, incident.getCategory()))
            .min(leastBusy)
            .orElseGet(() -> technicians.stream().min(leastBusy).orElse(null));
    }

    // Exact match on one skill in the comma-separated list (no more "contains" mistakes)
    public static boolean hasSkill(User tech, String category) {
        if (tech.getSkills() == null || category == null) return false;
        for (String s : tech.getSkills().split(",")) {
            if (s.trim().equalsIgnoreCase(category.trim())) return true;
        }
        return false;
    }
}