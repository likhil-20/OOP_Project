package com.itsm.ai;

public record TriageResult(String category, String impact, String urgency, Long serviceId,
                           int confidence, String reason, String source) {}
