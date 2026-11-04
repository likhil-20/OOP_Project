package com.itsm.observer;

import com.itsm.model.Incident;

public record IncidentEvent(Incident incident, String type, String actorEmail,
                            String previousState, String newState, String description) {}