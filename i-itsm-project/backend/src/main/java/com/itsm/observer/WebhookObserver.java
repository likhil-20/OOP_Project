package com.itsm.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsm.model.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Third notification channel: posts new P1 incidents to a Slack or Discord channel (optional). */
@Component
public class WebhookObserver implements IncidentObserver {

    private static final Logger log = LoggerFactory.getLogger(WebhookObserver.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${app.webhook.url:}")
    private String webhookUrl;

    @Override
    @Async
    public void update(IncidentEvent e) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        Incident inc = e.incident();
        if (!"INCIDENT_CREATED".equals(e.type()) || !"P1".equals(inc.getPriority())) return;

        String service = inc.getAffectedService() != null ? inc.getAffectedService().getServiceName() : "an unspecified service";
        String tech = inc.getAssignedTechnician() != null ? inc.getAssignedTechnician().getName() : "nobody yet";
        String desc = inc.getDescription().length() > 300 ? inc.getDescription().substring(0, 300) + "..." : inc.getDescription();
        String text = "P1 incident #" + inc.getId() + " on " + service + ". Assigned to " + tech + ". " + desc;

        try {
            // Slack reads "text", Discord reads "content"; each ignores the other key
            String json = MAPPER.writeValueAsString(Map.of("text", text, "content", text));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) log.warn("Webhook returned HTTP {}", res.statusCode());
        } catch (Exception ex) {
            log.warn("Webhook failed: {}", ex.getMessage());
        }
    }
}