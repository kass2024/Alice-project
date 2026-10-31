package com.alice.smartteacher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alice.smartteacher.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class GeminiService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.models:gemini-2.5-flash-lite,gemini-2.5-flash,gemini-flash-latest}")
    private String models;

    public boolean ready() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Map<String, Object> analyze(String track, String subject, String className,
                                        String primaryLabel, String primaryText,
                                        String secondaryLabel, String secondaryText) {
        if (!ready()) {
            throw new ApiException(400, "The Gemini API key is missing. Put it on one line in backend/gemini-key.local, then restart the API.");
        }
        String prompt = prompt(track, subject, className, primaryLabel, primaryText, secondaryLabel, secondaryText);
        String lastError = "Gemini did not return an analysis.";
        for (String model : models.split(",")) {
            String name = model.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                String text = call(name, prompt, true);
                Map<String, Object> json = parse(text);
                if (json != null) {
                    json.put("model", name);
                    return json;
                }
                lastError = "Gemini replied, but it was not valid JSON.";
            } catch (Exception first) {
                try {
                    String text = call(name, prompt, false);
                    Map<String, Object> json = parse(text);
                    if (json != null) {
                        json.put("model", name);
                        return json;
                    }
                    lastError = "Gemini replied, but it was not valid JSON.";
                } catch (Exception second) {
                    lastError = second.getMessage() == null ? first.getMessage() : second.getMessage();
                }
            }
        }
        throw new ApiException(502, lastError);
    }

    private String prompt(String track, String subject, String className,
                          String primaryLabel, String primaryText,
                          String secondaryLabel, String secondaryText) {
        boolean reb = "REB".equalsIgnoreCase(track);
        String guide = reb
                ? "This is a REB general-education class. The primary document is a Syllabus. The second document is a Weeks breakdown. Never call the weeks breakdown a chronogram."
                : "This is a TVET class. The primary document is a Curriculum. The second document is a Chronogram. Extract modules, learning outcomes, and indicative content.";
        return "You help a Rwandan secondary school build a scheme of work from teacher documents.\n"
                + guide + "\n"
                + "Subject: " + blank(subject) + "\n"
                + "Class: " + blank(className) + "\n\n"
                + primaryLabel + ":\n---\n" + blank(primaryText) + "\n---\n\n"
                + secondaryLabel + ":\n---\n" + blank(secondaryText) + "\n---\n\n"
                + "Return JSON only. Use this shape:\n"
                + "{\n"
                + "  \"summary\": \"one short paragraph\",\n"
                + "  \"items\": [{\"code\":\"\",\"title\":\"\",\"outcomes\":[\"\"],\"content\":[\"\"]}],\n"
                + "  \"weeks\": [{\"week\":1,\"topic\":\"\",\"hours\":1}],\n"
                + "  \"scheme\": [{\"week\":1,\"unit\":\"\",\"lesson\":\"\",\"objectives\":\"\",\"activities\":\"\",\"resources\":\"\",\"assessment\":\"\",\"hours\":1}]\n"
                + "}\n"
                + "For REB, items are units. For TVET, items are modules.\n"
                + "Build scheme rows by matching each week to the units or modules.\n"
                + "Use only what is written in the documents. Leave a field empty if it is not in the text.\n"
                + "No markdown.";
    }

    private String call(String model, String prompt, boolean jsonMode) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        contents.addObject().putArray("parts").addObject().put("text", prompt);
        ObjectNode gen = root.putObject("generationConfig");
        gen.put("temperature", 0.2);
        if (jsonMode) {
            gen.put("responseMimeType", "application/json");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", apiKey.trim())
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            String body = response.body() == null ? "" : response.body().replaceAll("\\s+", " ");
            if (body.length() > 300) {
                body = body.substring(0, 300);
            }
            throw new IllegalStateException("Gemini HTTP " + response.statusCode() + ": " + body);
        }
        JsonNode tree = mapper.readTree(response.body());
        if (tree.has("error")) {
            throw new IllegalStateException(tree.path("error").path("message").asText("Gemini error"));
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : tree.path("candidates").path(0).path("content").path("parts")) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            if (part.hasNonNull("text")) {
                text.append(part.get("text").asText());
            }
        }
        return text.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String raw = text.trim();
        if (raw.startsWith("```")) {
            raw = raw.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = raw.lastIndexOf("```");
            if (end >= 0) {
                raw = raw.substring(0, end);
            }
        }
        int start = raw.indexOf('{');
        int finish = raw.lastIndexOf('}');
        if (start < 0 || finish <= start) {
            return null;
        }
        try {
            return mapper.readValue(raw.substring(start, finish + 1), Map.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "(not provided)" : value.trim();
    }
}
