package com.itsm.strategy;

import com.itsm.model.Incident;
import com.itsm.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentStrategyTest {

    private User tech(String name, String skills, int load) {
        User u = new User();
        u.setName(name);
        u.setSkills(skills);
        u.setCurrentLoad(load);
        u.setRole(User.UserRole.TECHNICIAN);
        return u;
    }

    private Incident incident(String category) {
        Incident i = new Incident();
        i.setCategory(category);
        return i;
    }

    @Test
    @DisplayName("TC-13: Load-based strategy picks the technician with the lowest load")
    void loadBasedPicksLowestLoad() {
        User a = tech("A", "NETWORK", 3);
        User b = tech("B", "NETWORK", 1);
        User c = tech("C", "NETWORK", 2);

        User chosen = new LoadBasedAssignment().assignTechnician(incident("NETWORK"), List.of(a, b, c));

        assertEquals("B", chosen.getName());
    }

    @Test
    @DisplayName("TC-14: Skill-based strategy picks the technician whose skills match the category")
    void skillBasedPicksMatchingSkill() {
        User t1 = tech("T1", "NETWORK,DATABASE", 0);
        User t2 = tech("T2", "SOFTWARE,CLOUD", 0);

        User chosen = new SkillBasedAssignment().assignTechnician(incident("CLOUD"), List.of(t1, t2));

        assertEquals("T2", chosen.getName());
    }

    @Test
    @DisplayName("TC-15: Skill-based strategy falls back to the first technician when no skill matches")
    void skillBasedFallback() {
        User t1 = tech("T1", "NETWORK", 0);
        User t2 = tech("T2", "CLOUD", 0);

        User chosen = new SkillBasedAssignment().assignTechnician(incident("HARDWARE"), List.of(t1, t2));

        assertEquals("T1", chosen.getName());
    }
}