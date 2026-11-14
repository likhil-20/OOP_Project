package com.itsm.ai;

import com.itsm.model.ItService;
import java.util.List;

/** Contract for anything that can suggest category, impact, urgency and service for a report. */
public interface IncidentClassifier {
    TriageResult classify(String description, List<String> categories, List<ItService> services);
}
