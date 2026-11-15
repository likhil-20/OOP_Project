package com.itsm.strategy;

import com.itsm.model.Incident;
import com.itsm.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Picks the technician whose nearest SLA deadline is furthest away (most breathing room). */
public class DeadlineAwareAssignment implements AssignmentStrategy {

    // technician id -> earliest resolution deadline among their open incidents
    private final Map<Long, LocalDateTime> nearestDeadline;

    public DeadlineAwareAssignment(Map<Long, LocalDateTime> nearestDeadline) {
        this.nearestDeadline = nearestDeadline;
    }

    @Override
    public User assignTechnician(Incident incident, List<User> technicians) {
        if (technicians == null || technicians.isEmpty()) return null;

        // Prefer technicians with the right skill; if nobody has it, consider everyone
        List<User> skilled = technicians.stream()
            .filter(t -> SkillBasedAssignment.hasSkill(t, incident.getCategory()))
            .toList();
        List<User> pool = skilled.isEmpty() ? technicians : skilled;

        // No open work = most room. Otherwise the furthest "nearest deadline" wins. Tie: lower load.
        Comparator<User> mostRoom = Comparator
            .comparing((User t) -> nearestDeadline.getOrDefault(t.getId(), LocalDateTime.MAX))
            .reversed();
        return pool.stream().min(mostRoom.thenComparingInt(User::getCurrentLoad)).orElse(null);
    }
}