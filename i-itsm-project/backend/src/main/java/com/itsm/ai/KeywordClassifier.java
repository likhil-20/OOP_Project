package com.itsm.ai;

import com.itsm.model.ItService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Offline fallback: simple keyword rules. Used when Gemini is unavailable. */
@Component
public class KeywordClassifier implements IncidentClassifier {

    private static final Map<String, List<String>> KEYWORDS = Map.of(
        "NETWORK", List.of("wifi", "wi-fi", "internet", "network", "vpn", "router", "lan", "dns", "connectivity"),
        "HARDWARE", List.of("laptop", "keyboard", "mouse", "monitor", "printer", "screen", "battery", "hardware", "charger", "usb"),
        "SOFTWARE", List.of("install", "software", "update", "office", "excel", "word", "license", "freezes"),
        "DATABASE", List.of("database", "sql", "query", "table", "postgres", "mysql", "db"),
        "CLOUD", List.of("cloud", "aws", "azure", "gcp", "vm", "instance", "bucket", "kubernetes", "docker"),
        "SECURITY", List.of("virus", "malware", "phishing", "hack", "breach", "ransomware", "suspicious", "security"),
        "AUTHENTICATION", List.of("login", "log in", "sign in", "password", "sso", "oauth", "otp", "locked", "2fa"),
        "APPLICATION", List.of("app", "application", "page", "website", "portal", "500", "error", "checkout", "payment"),
        "STORAGE", List.of("disk", "storage", "drive", "backup", "space", "nas", "file server", "quota")
    );
    private static final List<String> URGENT = List.of("urgent", "immediately", "asap", "down", "outage",
        "cannot", "can't", "unable", "not working", "crash", "failing", "failed");
    private static final List<String> WIDE = List.of("everyone", "all users", "all employees", "entire",
        "whole", "production", "outage", "nobody", "no one");
    private static final List<String> MINOR = List.of("slow", "minor", "cosmetic", "when possible", "sometimes");
    private static final List<String> TEAM = List.of("team", "department", "floor", "block", "office",
        "several", "many", "multiple", "colleagues");
    private static final Set<String> GENERIC = Set.of("service", "application", "portal", "internal", "system");

    @Override
    public TriageResult classify(String description, List<String> categories, List<ItService> services) {
        String text = description.toLowerCase();
        Set<String> words = new HashSet<>(Arrays.asList(text.split("[^a-z0-9]+")));

        String best = null;
        int bestScore = 0;
        for (String cat : categories) {
            List<String> kws = KEYWORDS.getOrDefault(cat.toUpperCase(), List.of(cat.toLowerCase()));
            int score = 0;
            for (String kw : kws) if (matches(text, words, kw)) score++;
            if (score > bestScore) { best = cat; bestScore = score; }
        }

        String urgency = anyMatch(text, words, URGENT) ? "HIGH" : anyMatch(text, words, MINOR) ? "LOW" : "MEDIUM";
        String impact = anyMatch(text, words, WIDE) ? "HIGH"
            : (text.startsWith("my ") || text.contains(" my ")) ? "LOW" : "MEDIUM";

        Long serviceId = null;
        int bestService = 0;
        for (ItService s : services) {
            int hits = 0;
            for (String token : s.getServiceName().toLowerCase().split("[^a-z0-9]+")) {
                if (token.length() >= 2 && !GENERIC.contains(token) && words.contains(token)) hits++;
            }
            if (hits > bestService) { bestService = hits; serviceId = s.getId(); }
        }

        int confidence = bestScore == 0 ? 30 : Math.min(85, 50 + 15 * bestScore);
        int users = anyMatch(text, words, WIDE) ? 500 : anyMatch(text, words, TEAM) ? 20 : 1;
        String reason = best == null
            ? "No clear keywords found, so the category was left as Others."
            : "Keyword rules matched the " + best + " category.";
        return new TriageResult(best != null ? best : "Others", impact, urgency, serviceId,
            confidence, reason, "KEYWORD", users);
    }

    private static boolean matches(String text, Set<String> words, String kw) {
        boolean phrase = kw.contains(" ") || kw.contains("-") || kw.contains("'");
        return phrase ? text.contains(kw) : words.contains(kw);
    }

    private static boolean anyMatch(String text, Set<String> words, List<String> kws) {
        for (String kw : kws) if (matches(text, words, kw)) return true;
        return false;
    }
}
