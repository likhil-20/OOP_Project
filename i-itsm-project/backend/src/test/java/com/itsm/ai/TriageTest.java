package com.itsm.ai;


import com.itsm.model.IncidentCategory;
import com.itsm.repository.IncidentCategoryRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.service.TriageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageTest {

    private final List<String> categories = List.of("Network", "Hardware", "Software", "Database",
        "Cloud", "Security", "Authentication", "Application", "Storage");

    @Mock private GeminiClassifier gemini;
    @Mock private IncidentCategoryRepository categoryRepository;
    @Mock private ItServiceRepository serviceRepository;

    private IncidentCategory category(String name) {
        IncidentCategory c = new IncidentCategory();
        c.setName(name);
        return c;
    }

    @Test
    @DisplayName("TC-30: Keyword classifier picks Network and HIGH impact for a Wi-Fi outage affecting everyone")
    void keywordPicksNetwork() {
        TriageResult r = new KeywordClassifier().classify("Wi-Fi is down in block A for everyone", categories, List.of());
        assertEquals("Network", r.category());
        assertEquals("HIGH", r.impact());
        assertEquals("KEYWORD", r.source());
    }

    @Test
    @DisplayName("TC-31: Keyword classifier returns Others with low confidence when nothing matches")
    void keywordUnknownText() {
        TriageResult r = new KeywordClassifier().classify("xyzzy plugh", categories, List.of());
        assertEquals("Others", r.category());
        assertTrue(r.confidence() < 50);
    }

    @Test
    @DisplayName("TC-32: If Gemini fails, triage falls back to the keyword classifier")
    void fallsBackWhenGeminiFails() {
        TriageService service = new TriageService(gemini, new KeywordClassifier(), categoryRepository, serviceRepository);
        when(categoryRepository.findAll()).thenReturn(List.of(category("Network")));
        when(serviceRepository.findAll()).thenReturn(List.of());
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.classify(any(), any(), any())).thenThrow(new IllegalStateException("Gemini is down"));

        TriageResult r = service.triage("Wi-Fi is down");

        assertEquals("KEYWORD", r.source());
        assertEquals("Network", r.category());
    }

    @Test
    @DisplayName("TC-33: A Gemini answer is used and the same text is not sent to Gemini twice")
    void geminiResultIsCached() {
        TriageService service = new TriageService(gemini, new KeywordClassifier(), categoryRepository, serviceRepository);
        when(categoryRepository.findAll()).thenReturn(List.of(category("Network")));
        when(serviceRepository.findAll()).thenReturn(List.of());
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.classify(any(), any(), any()))
            .thenReturn(new TriageResult("Network", "HIGH", "HIGH", null, 90, "Wi-Fi outage", "GEMINI"));

        service.triage("Wi-Fi is down");
        TriageResult second = service.triage("  wi-fi is down ");

        assertEquals("GEMINI", second.source());
        verify(gemini, times(1)).classify(any(), any(), any());
    }

    @Test
    @DisplayName("TC-39: Keyword classifier estimates many affected users for a wide outage")
    void keywordEstimatesAffectedUsers() {
        TriageResult r = new KeywordClassifier().classify("Wi-Fi is down in block A for everyone", categories, List.of());
        assertTrue(r.affectedUsers() >= 100);
    }
}