package com.itsm.lifecycle;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class IncidentLifecycleManager {

    public enum State {
        NEW,
        CLASSIFIED,
        ASSIGNED,
        ACKNOWLEDGED,
        IN_PROGRESS,
        RESOLVED,
        VERIFIED,
        CLOSED
    }

    private static final Map<State, Set<State>> ALLOWED_TRANSITIONS = Map.of(
        State.NEW, EnumSet.of(State.CLASSIFIED),
        State.CLASSIFIED, EnumSet.of(State.ASSIGNED),
        State.ASSIGNED, EnumSet.of(State.ACKNOWLEDGED, State.IN_PROGRESS, State.RESOLVED),
        State.ACKNOWLEDGED, EnumSet.of(State.IN_PROGRESS, State.RESOLVED),
        State.IN_PROGRESS, EnumSet.of(State.RESOLVED),
        State.RESOLVED, EnumSet.of(State.VERIFIED, State.CLOSED),
        State.VERIFIED, EnumSet.of(State.CLOSED),
        State.CLOSED, EnumSet.noneOf(State.class)
    );

    public static boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        if (currentStatus.equalsIgnoreCase(targetStatus)) return true;
        try {
            State current = State.valueOf(currentStatus.toUpperCase());
            State target = State.valueOf(targetStatus.toUpperCase());
            Set<State> allowed = ALLOWED_TRANSITIONS.get(current);
            return allowed != null && allowed.contains(target);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
