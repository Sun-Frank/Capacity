package com.capics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MrpCompareAiService {

    private static final int MAX_SIGNAL_PER_QUERY = 3;
    private static final Pattern ITEM_PATTERN = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title><!\\[CDATA\\[(.*?)]]></title>|<title>(.*?)</title>", Pattern.DOTALL);
    private static final Pattern LINK_PATTERN = Pattern.compile("<link>(.*?)</link>", Pattern.DOTALL);
    private static final Pattern PUB_DATE_PATTERN = Pattern.compile("<pubDate>(.*?)</pubDate>", Pattern.DOTALL);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiAgentConfigService aiAgentConfigService;

    public MrpCompareAiService(ObjectMapper objectMapper, AiAgentConfigService aiAgentConfigService) {
        this.objectMapper = objectMapper;
        this.aiAgentConfigService = aiAgentConfigService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Map<String, Object> analyze(Map<String, Object> payload) {
        Map<String, Object> internalRiskContext = buildInternalCapacityRiskContext(payload);
        List<Map<String, Object>> externalSignals = fetchExternalSignals(payload);
        String fallback = buildRuleBasedAnalysis(payload, internalRiskContext, externalSignals);

        AiAgentConfigService.RuntimeConfig runtimeConfig = aiAgentConfigService.getRuntimeConfig();
        if (runtimeConfig.getApiKey() == null || runtimeConfig.getApiKey().trim().isEmpty()) {
            Map<String, Object> result = buildResult("local-rule", "rule-fallback", fallback,
                    "AI_AGENT_API_KEY is not configured. Rule-based analysis is used.");
            result.put("externalSignals", externalSignals);
            result.put("internalRisk", internalRiskContext);
            return result;
        }

        try {
            String aiText = callAgent(payload, runtimeConfig, internalRiskContext, externalSignals);
            if (aiText == null || aiText.trim().isEmpty()) {
                Map<String, Object> result = buildResult("local-rule", "rule-fallback", fallback,
                        "AI returned empty content. Rule-based analysis is used.");
                result.put("externalSignals", externalSignals);
                result.put("internalRisk", internalRiskContext);
                return result;
            }
            Map<String, Object> result = buildResult("ai-agent", runtimeConfig.getModel(), aiText.trim(), null);
            result.put("externalSignals", externalSignals);
            result.put("internalRisk", internalRiskContext);
            return result;
        } catch (Exception ex) {
            Map<String, Object> result = buildResult("local-rule", "rule-fallback", fallback,
                    "AI call failed. Fallback to rule-based analysis: " + ex.getMessage());
            result.put("externalSignals", externalSignals);
            result.put("internalRisk", internalRiskContext);
            return result;
        }
    }

    private String callAgent(
            Map<String, Object> payload,
            AiAgentConfigService.RuntimeConfig runtimeConfig,
            Map<String, Object> internalRiskContext,
            List<Map<String, Object>> externalSignals
    ) throws Exception {
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildAiSystemPrompt());

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", buildAiUserPrompt(payload, internalRiskContext, externalSignals));

        Map<String, Object> body = new HashMap<>();
        body.put("model", runtimeConfig.getModel());
        body.put("temperature", 0.2);
        body.put("messages", List.of(systemMsg, userMsg));

        String endpoint = resolveChatCompletionsEndpoint(runtimeConfig.getBaseUrl());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(Math.max(runtimeConfig.getTimeoutMs(), 10000)))
                .header("Authorization", "Bearer " + runtimeConfig.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if (response.statusCode() == 404) {
                String fallbackEndpoint = resolveResponsesEndpoint(runtimeConfig.getBaseUrl());
                HttpRequest fallbackRequest = HttpRequest.newBuilder()
                        .uri(URI.create(fallbackEndpoint))
                        .timeout(Duration.ofMillis(Math.max(runtimeConfig.getTimeoutMs(), 10000)))
                        .header("Authorization", "Bearer " + runtimeConfig.getApiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(buildResponsesBody(payload, runtimeConfig.getModel(), internalRiskContext, externalSignals)),
                                StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> fallbackResponse = httpClient.send(fallbackRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (fallbackResponse.statusCode() >= 200 && fallbackResponse.statusCode() < 300) {
                    JsonNode fallbackRoot = objectMapper.readTree(fallbackResponse.body());
                    String outText = fallbackRoot.path("output_text").asText("");
                    if (outText == null || outText.trim().isEmpty()) {
                        throw new RuntimeException("AI responses endpoint returned empty output_text");
                    }
                    return outText;
                }
                throw new RuntimeException("HTTP 404 (chat endpoint: " + endpoint + ", responses endpoint: " + fallbackEndpoint + ")");
            }
            throw new RuntimeException("HTTP " + response.statusCode() + " (endpoint: " + endpoint + ")");
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("AI response is missing choices");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    private String resolveChatCompletionsEndpoint(String baseUrl) {
        String raw = trimTrailingSlash(baseUrl);
        if (raw.endsWith("/chat/completions")) return raw;
        if (raw.endsWith("/responses")) {
            return raw.substring(0, raw.length() - "/responses".length()) + "/chat/completions";
        }
        return raw + "/chat/completions";
    }

    private String resolveResponsesEndpoint(String baseUrl) {
        String raw = trimTrailingSlash(baseUrl);
        if (raw.endsWith("/responses")) return raw;
        if (raw.endsWith("/chat/completions")) {
            return raw.substring(0, raw.length() - "/chat/completions".length()) + "/responses";
        }
        return raw + "/responses";
    }

    private Map<String, Object> buildResponsesBody(
            Map<String, Object> payload,
            String model,
            Map<String, Object> internalRiskContext,
            List<Map<String, Object>> externalSignals
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("input", buildAiSystemPrompt() + "\n\n" + buildAiUserPrompt(payload, internalRiskContext, externalSignals));
        return body;
    }

    private List<Map<String, Object>> fetchExternalSignals(Map<String, Object> payload) {
        List<Map<String, Object>> signals = new ArrayList<>();
        List<String> queries = new ArrayList<>();
        queries.add("china auto market sales demand trend");
        queries.add("automotive electronics chip supply chain risk");

        for (String focus : extractFocusItemNames(payload)) {
            queries.add("automotive supply risk " + focus);
        }

        int queryCount = 0;
        for (String query : queries) {
            if (queryCount >= 4) break;
            queryCount++;
            try {
                String url = "https://news.google.com/rss/search?q="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8)
                        + "&hl=en-US&gl=US&ceid=US:en";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "CAPICS-MRP-AI/1.0")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    signals.addAll(parseRssItems(response.body(), query, MAX_SIGNAL_PER_QUERY));
                }
            } catch (Exception ignore) {
                // best effort only
            }
        }

        if (signals.isEmpty()) {
            Map<String, Object> noSignal = new HashMap<>();
            noSignal.put("source", "internet");
            noSignal.put("note", "No external signals fetched at runtime. Use MRP pattern inference.");
            signals.add(noSignal);
        }

        return signals;
    }

    private List<Map<String, Object>> parseRssItems(String xml, String query, int maxCount) {
        List<Map<String, Object>> results = new ArrayList<>();
        Matcher itemMatcher = ITEM_PATTERN.matcher(xml);
        int count = 0;
        while (itemMatcher.find() && count < maxCount) {
            String block = itemMatcher.group(1);
            String title = extractPatternValue(block, TITLE_PATTERN);
            String link = extractPatternValue(block, LINK_PATTERN);
            String pubDate = extractPatternValue(block, PUB_DATE_PATTERN);
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("source", "google-news-rss");
            item.put("query", query);
            item.put("title", normalizeText(title));
            item.put("link", normalizeText(link));
            item.put("publishedAt", normalizeText(pubDate));
            results.add(item);
            count++;
        }
        return results;
    }

    private String extractPatternValue(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        if (matcher.groupCount() >= 2) {
            String g1 = matcher.group(1);
            if (g1 != null && !g1.trim().isEmpty()) return g1;
            String g2 = matcher.group(2);
            if (g2 != null) return g2;
        }
        String g0 = matcher.group(1);
        return g0 == null ? "" : g0;
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return value.replaceAll("<.*?>", "").replace("&amp;", "&").trim();
    }

    private List<String> extractFocusItemNames(Map<String, Object> payload) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> item : asListOfMap(payload.get("topItems"))) {
            String name = asString(item.get("name"), "");
            if (!name.isEmpty()) {
                names.add(name);
            }
            if (names.size() >= 2) break;
        }
        return names;
    }

    private Map<String, Object> buildInternalCapacityRiskContext(Map<String, Object> payload) {
        Map<String, Object> summary = asMap(payload.get("summary"));
        BigDecimal totalA = asDecimal(summary.get("totalQtyA"));
        BigDecimal totalB = asDecimal(summary.get("totalQtyB"));
        BigDecimal totalDelta = asDecimal(summary.get("totalDelta"));

        List<Map<String, Object>> topItems = asListOfMap(payload.get("topItems"));
        int increaseItemCount = 0;
        int decreaseItemCount = 0;
        int largeChangeItemCount = 0;
        BigDecimal totalAbsItemDelta = BigDecimal.ZERO;
        BigDecimal top3AbsDelta = BigDecimal.ZERO;
        BigDecimal largeChangeThreshold = totalDelta.abs().multiply(new BigDecimal("0.08")).max(new BigDecimal("100"));

        for (int i = 0; i < topItems.size(); i++) {
            BigDecimal delta = asDecimal(topItems.get(i).get("totalDelta"));
            BigDecimal abs = delta.abs();
            totalAbsItemDelta = totalAbsItemDelta.add(abs);
            if (i < 3) top3AbsDelta = top3AbsDelta.add(abs);
            if (delta.compareTo(BigDecimal.ZERO) > 0) increaseItemCount++;
            else if (delta.compareTo(BigDecimal.ZERO) < 0) decreaseItemCount++;
            if (abs.compareTo(largeChangeThreshold) >= 0) largeChangeItemCount++;
        }

        BigDecimal totalDeltaRate = safePercent(totalDelta.abs(), totalA.abs().max(BigDecimal.ONE));
        BigDecimal concentrationRate = safePercent(top3AbsDelta, totalAbsItemDelta.max(BigDecimal.ONE));
        BigDecimal periodShockRate = calcPeriodShockRate(topItems, totalB);

        int marketRiskScore = scoreMarketRisk(totalDeltaRate, increaseItemCount, decreaseItemCount, periodShockRate);
        int supplyRiskScore = scoreSupplyRisk(concentrationRate, largeChangeItemCount, topItems.size(), periodShockRate);
        int capacityRiskScore = scoreCapacityRisk(totalDelta, totalB, increaseItemCount, topItems.size(), periodShockRate);

        Map<String, Object> context = new HashMap<>();
        context.put("totalDeltaRatePercent", totalDeltaRate);
        context.put("top3ConcentrationPercent", concentrationRate);
        context.put("periodShockPercent", periodShockRate);
        context.put("increaseItemCount", increaseItemCount);
        context.put("decreaseItemCount", decreaseItemCount);
        context.put("marketRiskScore", marketRiskScore);
        context.put("supplyRiskScore", supplyRiskScore);
        context.put("capacityRiskScore", capacityRiskScore);
        context.put("marketRiskLevel", riskLevel(marketRiskScore));
        context.put("supplyRiskLevel", riskLevel(supplyRiskScore));
        context.put("capacityRiskLevel", riskLevel(capacityRiskScore));
        return context;
    }

    private String buildRuleBasedAnalysis(
            Map<String, Object> payload,
            Map<String, Object> internalRiskContext,
            List<Map<String, Object>> externalSignals
    ) {
        String viewType = asString(payload.get("viewType"), "week");
        String fileLabelA = asString(payload.get("fileLabelA"), "File A");
        String fileLabelB = asString(payload.get("fileLabelB"), "File B");
        Map<String, Object> summary = asMap(payload.get("summary"));

        BigDecimal totalA = asDecimal(summary.get("totalQtyA"));
        BigDecimal totalB = asDecimal(summary.get("totalQtyB"));
        BigDecimal totalDelta = asDecimal(summary.get("totalDelta"));

        StringBuilder sb = new StringBuilder();
        sb.append("Integrated analysis (fallback rule mode)\n");
        sb.append("- Window: ").append("month".equalsIgnoreCase(viewType) ? "Monthly" : "Weekly").append("\n");
        sb.append("- Compare: ").append(fileLabelA).append(" vs ").append(fileLabelB).append("\n");
        sb.append("- Total A/B/Delta: ").append(format(totalA)).append(" / ").append(format(totalB)).append(" / ").append(withSign(totalDelta)).append("\n");

        sb.append("- Internal factory capacity risk: ")
                .append(asString(internalRiskContext.get("capacityRiskLevel"), "N/A"))
                .append(" (").append(asString(internalRiskContext.get("capacityRiskScore"), "0")).append("/100)")
                .append(", period shock ").append(formatPercent(asDecimal(internalRiskContext.get("periodShockPercent")))).append("\n");

        sb.append("- Inferred market risk: ")
                .append(asString(internalRiskContext.get("marketRiskLevel"), "N/A"))
                .append(" (").append(asString(internalRiskContext.get("marketRiskScore"), "0")).append("/100)")
                .append(", delta rate ").append(formatPercent(asDecimal(internalRiskContext.get("totalDeltaRatePercent")))).append("\n");

        sb.append("- Inferred supply-chain risk: ")
                .append(asString(internalRiskContext.get("supplyRiskLevel"), "N/A"))
                .append(" (").append(asString(internalRiskContext.get("supplyRiskScore"), "0")).append("/100)")
                .append(", concentration ").append(formatPercent(asDecimal(internalRiskContext.get("top3ConcentrationPercent")))).append("\n");

        sb.append("- External signals fetched: ").append(externalSignals.size()).append("\n");
        int shown = 0;
        for (Map<String, Object> signal : externalSignals) {
            String title = asString(signal.get("title"), "");
            if (title.isEmpty()) continue;
            sb.append("  - ").append(title).append("\n");
            shown++;
            if (shown >= 3) break;
        }

        sb.append("Conclusion: combine internet market/supply signals with internal capacity metrics to prioritize delta items and release mitigation actions.");
        return sb.toString();
    }

    private String buildAiSystemPrompt() {
        return "You are an MRP variance analyst."
                + " During analysis, you must combine two sources: "
                + "(1) internet signals for market trend and automotive electronics supply chain risk, "
                + "(2) internal system metrics for factory capacity risk. "
                + "Do NOT force a rigid section template. Produce a practical integrated conclusion in Chinese. "
                + "If internet signals are missing, explicitly state that external signals were unavailable and continue with best-effort inference.";
    }

    private String buildAiUserPrompt(
            Map<String, Object> payload,
            Map<String, Object> internalRiskContext,
            List<Map<String, Object>> externalSignals
    ) throws Exception {
        Map<String, Object> combined = new HashMap<>();
        combined.put("mrpPayload", payload);
        combined.put("internalFactoryCapacityRisk", internalRiskContext);
        combined.put("internetSignals", externalSignals);
        combined.put("analysisRequirement", "Integrate internet + internal signals and provide final conclusion with evidence and actions.");

        return "Please analyze MRP variance using combined context below. "
                + "No rigid heading is required. Focus on useful conclusion and actions.\n\n"
                + objectMapper.writeValueAsString(combined);
    }

    private BigDecimal calcPeriodShockRate(List<Map<String, Object>> topItems, BigDecimal totalB) {
        if (topItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> periodDeltas = new ArrayList<>();
        for (Map<String, Object> item : topItems) {
            Object valuesObj = item.get("periodDeltas");
            if (!(valuesObj instanceof List)) {
                continue;
            }
            List<?> values = (List<?>) valuesObj;
            for (int i = 0; i < values.size(); i++) {
                if (periodDeltas.size() <= i) {
                    periodDeltas.add(BigDecimal.ZERO);
                }
                periodDeltas.set(i, periodDeltas.get(i).add(asDecimal(values.get(i))));
            }
        }
        BigDecimal maxShock = BigDecimal.ZERO;
        for (BigDecimal value : periodDeltas) {
            if (value.abs().compareTo(maxShock) > 0) {
                maxShock = value.abs();
            }
        }
        return safePercent(maxShock, totalB.abs().max(BigDecimal.ONE));
    }

    private int scoreMarketRisk(BigDecimal totalDeltaRate, int increaseCount, int decreaseCount, BigDecimal periodShockRate) {
        int score = 30;
        double rate = totalDeltaRate.doubleValue();
        if (rate >= 20) score += 30;
        else if (rate >= 10) score += 20;
        else if (rate >= 5) score += 10;

        int total = Math.max(increaseCount + decreaseCount, 1);
        double imbalance = Math.abs(increaseCount - decreaseCount) * 1.0 / total;
        if (imbalance >= 0.7) score += 15;
        else if (imbalance >= 0.4) score += 8;

        if (periodShockRate.doubleValue() >= 12) score += 20;
        else if (periodShockRate.doubleValue() >= 6) score += 10;

        return clampScore(score);
    }

    private int scoreSupplyRisk(BigDecimal concentrationRate, int largeChangeCount, int totalItems, BigDecimal periodShockRate) {
        int score = 25;
        double concentration = concentrationRate.doubleValue();
        if (concentration >= 65) score += 35;
        else if (concentration >= 50) score += 25;
        else if (concentration >= 35) score += 12;

        double ratio = totalItems == 0 ? 0 : (largeChangeCount * 1.0 / totalItems);
        if (ratio >= 0.45) score += 25;
        else if (ratio >= 0.25) score += 15;
        else if (ratio >= 0.1) score += 8;

        if (periodShockRate.doubleValue() >= 10) score += 15;
        else if (periodShockRate.doubleValue() >= 5) score += 8;

        return clampScore(score);
    }

    private int scoreCapacityRisk(BigDecimal totalDelta, BigDecimal totalB, int increaseCount, int totalItems, BigDecimal periodShockRate) {
        int score = 20;
        if (totalDelta.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal growthVsB = safePercent(totalDelta, totalB.abs().max(BigDecimal.ONE));
            double growth = growthVsB.doubleValue();
            if (growth >= 20) score += 35;
            else if (growth >= 10) score += 25;
            else if (growth >= 5) score += 12;
        } else {
            score += 5;
        }

        double increaseRatio = totalItems == 0 ? 0 : (increaseCount * 1.0 / totalItems);
        if (increaseRatio >= 0.6) score += 20;
        else if (increaseRatio >= 0.35) score += 12;

        if (periodShockRate.doubleValue() >= 12) score += 25;
        else if (periodShockRate.doubleValue() >= 6) score += 12;

        return clampScore(score);
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(score, 100));
    }

    private String riskLevel(int score) {
        if (score >= 75) return "High";
        if (score >= 50) return "Medium";
        return "Low";
    }

    private Map<String, Object> buildResult(String provider, String modelName, String analysis, String note) {
        Map<String, Object> result = new HashMap<>();
        result.put("provider", provider);
        result.put("model", modelName);
        result.put("analysis", analysis);
        result.put("note", note);
        return result;
    }

    private String trimTrailingSlash(String value) {
        String raw = value == null ? "" : value.trim();
        while (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        if (!(value instanceof List)) {
            return List.of();
        }
        List<?> list = (List<?>) value;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private String asString(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private int asInt(Object value) {
        try {
            if (value == null) return 0;
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private BigDecimal asDecimal(Object value) {
        try {
            if (value == null) return BigDecimal.ZERO;
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(new BigDecimal("100"))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String withSign(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + format(value);
        }
        return format(value);
    }

    private String format(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
