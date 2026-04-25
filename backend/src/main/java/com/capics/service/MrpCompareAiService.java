package com.capics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MrpCompareAiService {

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
        String fallback = buildRuleBasedAnalysis(payload);
        AiAgentConfigService.RuntimeConfig runtimeConfig = aiAgentConfigService.getRuntimeConfig();

        if (runtimeConfig.getApiKey() == null || runtimeConfig.getApiKey().trim().isEmpty()) {
            return buildResult("local-rule", "rule-fallback", fallback, "AI_AGENT_API_KEY 未配置，已使用规则分析。");
        }

        try {
            String aiText = callAgent(payload, runtimeConfig);
            if (aiText == null || aiText.trim().isEmpty()) {
                return buildResult("local-rule", "rule-fallback", fallback, "AI返回为空，已使用规则分析。");
            }
            return buildResult("ai-agent", runtimeConfig.getModel(), aiText.trim(), null);
        } catch (Exception ex) {
            return buildResult("local-rule", "rule-fallback", fallback, "AI调用失败，已降级规则分析: " + ex.getMessage());
        }
    }

    private String callAgent(Map<String, Object> payload, AiAgentConfigService.RuntimeConfig runtimeConfig) throws Exception {
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是制造业MRP分析助手。请基于输入数据输出结构化中文分析，包含：1) 总体判断 2) 差异最大项 3) 可能原因 4) 风险 5) 建议动作。语言简洁，使用条目。");

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", objectMapper.writeValueAsString(payload));

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
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildResponsesBody(payload, runtimeConfig.getModel())), StandardCharsets.UTF_8))
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
            throw new RuntimeException("AI响应缺少choices");
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

    private Map<String, Object> buildResponsesBody(Map<String, Object> payload, String model) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("input", "你是制造业MRP分析助手。请基于输入数据输出结构化中文分析，包含：1) 总体判断 2) 差异最大项 3) 可能原因 4) 风险 5) 建议动作。语言简洁，使用条目。\n\n输入数据：" + objectMapper.writeValueAsString(payload));
        return body;
    }

    private String buildRuleBasedAnalysis(Map<String, Object> payload) {
        String viewType = asString(payload.get("viewType"), "week");
        String fileLabelA = asString(payload.get("fileLabelA"), "文件A");
        String fileLabelB = asString(payload.get("fileLabelB"), "文件B");

        Map<String, Object> summary = asMap(payload.get("summary"));
        BigDecimal totalA = asDecimal(summary.get("totalQtyA"));
        BigDecimal totalB = asDecimal(summary.get("totalQtyB"));
        BigDecimal totalDelta = asDecimal(summary.get("totalDelta"));
        int itemCount = asInt(summary.get("itemCount"));

        List<Map<String, Object>> topItems = asListOfMap(payload.get("topItems"));
        Map<String, Object> biggestIncrease = null;
        Map<String, Object> biggestDecrease = null;

        for (Map<String, Object> item : topItems) {
            BigDecimal delta = asDecimal(item.get("totalDelta"));
            if (delta.compareTo(BigDecimal.ZERO) >= 0) {
                if (biggestIncrease == null || delta.compareTo(asDecimal(biggestIncrease.get("totalDelta"))) > 0) {
                    biggestIncrease = item;
                }
            } else {
                if (biggestDecrease == null || delta.compareTo(asDecimal(biggestDecrease.get("totalDelta"))) < 0) {
                    biggestDecrease = item;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("一、总体判断\n");
        sb.append("- 对比维度：").append("month".equalsIgnoreCase(viewType) ? "月度" : "周度").append("\n");
        sb.append("- 对比对象：").append(fileLabelA).append(" vs ").append(fileLabelB).append("\n");
        sb.append("- 汇总项目数：").append(itemCount).append("\n");
        sb.append("- ").append(fileLabelA).append("总量：").append(format(totalA)).append("\n");
        sb.append("- ").append(fileLabelB).append("总量：").append(format(totalB)).append("\n");
        sb.append("- 总差异（B-A）：").append(withSign(totalDelta)).append("\n\n");

        sb.append("二、差异重点\n");
        if (biggestIncrease != null) {
            sb.append("- 最大增加项：").append(asString(biggestIncrease.get("name"), "N/A"))
                    .append("，差异 ").append(withSign(asDecimal(biggestIncrease.get("totalDelta")))).append("\n");
        }
        if (biggestDecrease != null) {
            sb.append("- 最大减少项：").append(asString(biggestDecrease.get("name"), "N/A"))
                    .append("，差异 ").append(withSign(asDecimal(biggestDecrease.get("totalDelta")))).append("\n");
        }
        if (biggestIncrease == null && biggestDecrease == null) {
            sb.append("- 未识别到显著差异项。\n");
        }

        sb.append("\n三、可能原因\n");
        sb.append("- 文件版本切换导致需求窗口或需求分布变化。\n");
        sb.append("- 部分产品描述归并后，原始Item Number映射变化。\n");
        sb.append("- 上游预测/订单结构调整，导致阶段性拉高或回落。\n");

        sb.append("\n四、建议动作\n");
        sb.append("- 先锁定差异绝对值Top项，逐项核对需求来源与版本变更说明。\n");
        sb.append("- 对持续增加项评估产能与物料风险，对持续减少项评估呆滞与排产切换影响。\n");
        sb.append("- 将本次差异结论沉淀为下一版MRP发布前的检查清单。\n");

        return sb.toString();
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

    private String withSign(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + format(value);
        }
        return format(value);
    }

    private String format(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
