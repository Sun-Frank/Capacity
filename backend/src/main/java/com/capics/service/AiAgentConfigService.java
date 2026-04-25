package com.capics.service;

import com.capics.dto.AiAgentConfigDto;
import com.capics.entity.AiAgentConfig;
import com.capics.repository.AiAgentConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAgentConfigService {

    private static final int CONFIG_ID = 1;

    private final AiAgentConfigRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${AI_AGENT_API_KEY:}")
    private String envApiKey;

    @Value("${AI_AGENT_BASE_URL:https://api.openai.com/v1}")
    private String envBaseUrl;

    @Value("${AI_AGENT_MODEL:gpt-4o-mini}")
    private String envModel;

    @Value("${AI_AGENT_TIMEOUT_MS:45000}")
    private Integer envTimeoutMs;

    public AiAgentConfigService(AiAgentConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public RuntimeConfig getRuntimeConfig() {
        AiAgentConfig stored = repository.findById(CONFIG_ID).orElse(null);

        String apiKey = coalesce(stored == null ? null : stored.getApiKey(), envApiKey);
        String baseUrl = coalesce(stored == null ? null : stored.getBaseUrl(), envBaseUrl);
        String model = coalesce(stored == null ? null : stored.getModel(), envModel);
        Integer timeoutMs = (stored != null && stored.getTimeoutMs() != null && stored.getTimeoutMs() > 0)
                ? stored.getTimeoutMs()
                : (envTimeoutMs != null && envTimeoutMs > 0 ? envTimeoutMs : 45000);

        return new RuntimeConfig(apiKey, baseUrl, model, timeoutMs);
    }

    public AiAgentConfigDto getUiConfig() {
        AiAgentConfig stored = repository.findById(CONFIG_ID).orElse(null);
        RuntimeConfig runtime = getRuntimeConfig();

        AiAgentConfigDto dto = new AiAgentConfigDto();
        dto.setHasApiKey(isNotBlank(runtime.getApiKey()));
        dto.setApiKeyMasked(mask(runtime.getApiKey()));
        dto.setBaseUrl(runtime.getBaseUrl());
        dto.setModel(runtime.getModel());
        dto.setTimeoutMs(runtime.getTimeoutMs());
        dto.setSource(stored == null ? "env" : "db");

        if (stored != null) {
            dto.setUpdatedBy(stored.getUpdatedBy());
            dto.setUpdatedAt(stored.getUpdatedAt() == null ? null : stored.getUpdatedAt().toString());
        }

        return dto;
    }

    @Transactional
    public AiAgentConfigDto save(AiAgentConfigDto input, String updatedBy) {
        AiAgentConfig entity = repository.findById(CONFIG_ID).orElseGet(() -> {
            AiAgentConfig created = new AiAgentConfig();
            created.setId(CONFIG_ID);
            return created;
        });

        if (input.getApiKey() != null) {
            entity.setApiKey(trimToNull(input.getApiKey()));
        }
        entity.setBaseUrl(trimToNull(input.getBaseUrl()));
        entity.setModel(trimToNull(input.getModel()));
        if (input.getTimeoutMs() != null && input.getTimeoutMs() > 0) {
            entity.setTimeoutMs(input.getTimeoutMs());
        }
        entity.setUpdatedBy(trimToNull(updatedBy) == null ? "system" : updatedBy);

        repository.save(entity);
        return getUiConfig();
    }

    public Map<String, Object> testConnection(AiAgentConfigDto input) {
        RuntimeConfig runtime = getRuntimeConfig();
        String apiKey = pickInputOrDefault(input == null ? null : input.getApiKey(), runtime.getApiKey(), true);
        String baseUrl = pickInputOrDefault(input == null ? null : input.getBaseUrl(), runtime.getBaseUrl(), false);
        String model = pickInputOrDefault(input == null ? null : input.getModel(), runtime.getModel(), false);
        Integer timeoutMs = (input != null && input.getTimeoutMs() != null && input.getTimeoutMs() > 0)
                ? input.getTimeoutMs()
                : runtime.getTimeoutMs();

        Map<String, Object> result = new HashMap<>();
        result.put("baseUrl", baseUrl);
        result.put("model", model);
        result.put("timeoutMs", timeoutMs);

        if (!isNotBlank(apiKey)) {
            result.put("ok", false);
            result.put("message", "API Key 为空，无法测试");
            return result;
        }

        try {
            long startedAt = System.currentTimeMillis();

            String chatEndpoint = resolveChatCompletionsEndpoint(baseUrl);
            HttpResponse<String> chatResp = sendChatProbe(chatEndpoint, apiKey, model, timeoutMs);
            if (isHttpOk(chatResp.statusCode())) {
                return buildSuccessResult(result, chatEndpoint, chatResp.statusCode(), startedAt, "Chat Completions 连通成功");
            }

            if (chatResp.statusCode() == 404) {
                String responsesEndpoint = resolveResponsesEndpoint(baseUrl);
                HttpResponse<String> resp = sendResponsesProbe(responsesEndpoint, apiKey, model, timeoutMs);
                if (isHttpOk(resp.statusCode())) {
                    return buildSuccessResult(result, responsesEndpoint, resp.statusCode(), startedAt, "Responses 连通成功");
                }
                return buildFailureResult(result, responsesEndpoint, resp, startedAt);
            }

            return buildFailureResult(result, chatEndpoint, chatResp, startedAt);
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("message", "连接测试异常: " + ex.getMessage());
            return result;
        }
    }

    private Map<String, Object> buildSuccessResult(
            Map<String, Object> base,
            String endpoint,
            int status,
            long startedAt,
            String message
    ) {
        base.put("ok", true);
        base.put("endpoint", endpoint);
        base.put("status", status);
        base.put("latencyMs", System.currentTimeMillis() - startedAt);
        base.put("message", message);
        return base;
    }

    private Map<String, Object> buildFailureResult(
            Map<String, Object> base,
            String endpoint,
            HttpResponse<String> response,
            long startedAt
    ) {
        int status = response.statusCode();
        base.put("ok", false);
        base.put("endpoint", endpoint);
        base.put("status", status);
        base.put("latencyMs", System.currentTimeMillis() - startedAt);

        Map<String, String> errorInfo = parseProviderError(response.body());
        String errorType = errorInfo.get("type");
        String errorCode = errorInfo.get("code");
        String providerMessage = errorInfo.get("providerMessage");

        if (isNotBlank(errorType)) {
            base.put("errorType", errorType);
        }
        if (isNotBlank(errorCode)) {
            base.put("errorCode", errorCode);
        }
        if (isNotBlank(providerMessage)) {
            base.put("providerMessage", providerMessage);
        }

        base.put("message", buildFriendlyMessage(status, errorType, errorCode, providerMessage));
        return base;
    }

    private String buildFriendlyMessage(int status, String errorType, String errorCode, String providerMessage) {
        if (status == 401) {
            return "连接失败：API Key 无效或已过期（401）";
        }
        if (status == 403) {
            return "连接失败：当前账号无权限访问该模型或接口（403）";
        }
        if (status == 404) {
            return "连接失败：接口地址不存在（404），请检查 Base URL";
        }
        if (status == 429) {
            if ("insufficient_quota".equalsIgnoreCase(errorCode) || "insufficient_quota".equalsIgnoreCase(errorType)) {
                return "连接失败：OpenAI 额度不足（insufficient_quota）";
            }
            if ("rate_limit_exceeded".equalsIgnoreCase(errorCode) || "rate_limit_exceeded".equalsIgnoreCase(errorType)) {
                return "连接失败：触发速率限制（rate_limit_exceeded），请稍后重试";
            }
            if (isNotBlank(providerMessage)) {
                return "连接失败（429）：" + providerMessage;
            }
            return "连接失败：请求过于频繁或额度不足（429）";
        }
        if (status >= 500) {
            return "连接失败：上游服务异常（HTTP " + status + "）";
        }
        if (isNotBlank(providerMessage)) {
            return "连接失败（HTTP " + status + "）：" + providerMessage;
        }
        return "连接失败（HTTP " + status + "）";
    }

    private Map<String, String> parseProviderError(String responseBody) {
        Map<String, String> data = new HashMap<>();
        if (!isNotBlank(responseBody)) {
            return data;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");

            String type = textOrNull(errorNode, "type");
            String code = textOrNull(errorNode, "code");
            String message = textOrNull(errorNode, "message");

            if (!isNotBlank(message)) {
                message = textOrNull(root, "message");
            }
            if (!isNotBlank(type)) {
                type = textOrNull(root, "type");
            }
            if (!isNotBlank(code)) {
                code = textOrNull(root, "code");
            }

            if (isNotBlank(type)) {
                data.put("type", type);
            }
            if (isNotBlank(code)) {
                data.put("code", code);
            }
            if (isNotBlank(message)) {
                data.put("providerMessage", message);
            }
        } catch (Exception ignored) {
            // Not JSON, skip structured parsing.
        }
        return data;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String text = v.asText();
        return trimToNull(text);
    }

    private boolean isHttpOk(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private HttpResponse<String> sendChatProbe(String endpoint, String apiKey, String model, Integer timeoutMs) throws Exception {
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "Reply with OK.");
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "ping");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(systemMsg, userMsg));
        body.put("temperature", 0);
        body.put("max_tokens", 8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(Math.max(timeoutMs == null ? 45000 : timeoutMs, 10000)))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendResponsesProbe(String endpoint, String apiKey, String model, Integer timeoutMs) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", "Reply with OK.");
        body.put("temperature", 0);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(Math.max(timeoutMs == null ? 45000 : timeoutMs, 10000)))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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

    private String trimTrailingSlash(String value) {
        String raw = value == null ? "" : value.trim();
        while (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    private String pickInputOrDefault(String input, String fallback, boolean allowEmptyAsClear) {
        if (input == null) return fallback;
        String trimmed = input.trim();
        if (allowEmptyAsClear && trimmed.isEmpty()) return "";
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String coalesce(String first, String second) {
        return isNotBlank(first) ? first.trim() : (second == null ? "" : second.trim());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String mask(String value) {
        if (!isNotBlank(value)) return "";
        String raw = value.trim();
        if (raw.length() <= 8) return "********";
        return raw.substring(0, 4) + "****" + raw.substring(raw.length() - 4);
    }

    public static class RuntimeConfig {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final Integer timeoutMs;

        public RuntimeConfig(String apiKey, String baseUrl, String model, Integer timeoutMs) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.timeoutMs = timeoutMs;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getModel() {
            return model;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }
    }
}
