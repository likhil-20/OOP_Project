package com.itsm.observer;

public interface IncidentObserver {
    void update(IncidentEvent event);
}