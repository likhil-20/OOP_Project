package com.itsm.ai;

public record TriageResult(String category, String impact, String urgency, Long serviceId,
                           int confidence, String reason, String source, Integer affectedUsers) {

    // Keeps the older 7-argument calls working (affected users defaults to 1)
    public TriageResult(String category, String impact, String urgency, Long serviceId,
                        int confidence, String reason, String source) {
        this(category, impact, urgency, serviceId, confidence, reason, source, 1);
    }
}