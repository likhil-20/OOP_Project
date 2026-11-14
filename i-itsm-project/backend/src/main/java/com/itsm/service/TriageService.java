package com.itsm.service;

import com.itsm.ai.GeminiClassifier;
import com.itsm.ai.KeywordClassifier;
import com.itsm.ai.TriageResult;
import com.itsm.exception.BadRequestException;
import com.itsm.model.IncidentCategory;
import com.itsm.model.ItService;
import com.itsm.repository.IncidentCategoryRepository;
import com.itsm.repository.ItServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tries Gemini first. If it is missing or fails, the keyword rules answer instead. */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final GeminiClassifier gemini;
    private final KeywordClassifier keyword;
    private final IncidentCategoryRepository categoryRepository;
    private final ItServiceRepository serviceRepository;

    // Small cache of Gemini answers: saves free-tier quota when the same text is analysed again
    private final Map<String, TriageResult> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TriageResult> eldest) {
                return size() > 200;
            }
        });

    @Autowired
    public TriageService(GeminiClassifier gemini, KeywordClassifier keyword,
                         IncidentCategoryRepository categoryRepository, ItServiceRepository serviceRepository) {
        this.gemini = gemini;
        this.keyword = keyword;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
    }

    public TriageResult triage(String description) {
        String text = description.trim();
        String key = text.toLowerCase();
        TriageResult cached = cache.get(key);
        if (cached != null) return cached;

        List<String> categories = categoryRepository.findAll().stream().map(IncidentCategory::getName).toList();
        List<ItService> services = serviceRepository.findAll();

        if (gemini.isConfigured()) {
            try {
                TriageResult result = gemini.classify(text, categories, services);
                cache.put(key, result);
                return result;
            } catch (Exception e) {
                log.warn("Gemini triage failed, using keyword fallback: {}", e.getMessage());
            }
        }
        return keyword.classify(text, categories, services);
    }
    
    public String transcribe(String base64Audio, String mimeType) {
        if (!gemini.isConfigured()) {
            throw new BadRequestException("Voice input needs the Gemini key. Please type the problem instead.");
        }
        try {
            return gemini.transcribe(base64Audio, mimeType);
        } catch (Exception e) {
            log.warn("Transcription failed: {}", e.getMessage());
            throw new BadRequestException("Could not turn the recording into text. Please try again or type it.");
        }
    }
}