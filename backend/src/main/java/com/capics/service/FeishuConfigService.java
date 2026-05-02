package com.capics.service;

import com.capics.dto.FeishuConfigDto;
import com.capics.dto.FeishuMessageRequest;
import com.capics.entity.FeishuConfig;
import com.capics.repository.FeishuConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class FeishuConfigService {

    private static final int CONFIG_ID = 1;
    private static final String DEFAULT_API_URL = "https://open.feishu.cn";

    private final FeishuConfigRepository repository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public FeishuConfigService(FeishuConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public FeishuConfigDto getUiConfig() {
        FeishuConfig entity = repository.findById(CONFIG_ID).orElse(null);
        FeishuConfigDto dto = new FeishuConfigDto();
        dto.setApiUrl(entity == null || isBlank(entity.getApiUrl()) ? DEFAULT_API_URL : entity.getApiUrl());
        dto.setAppId(entity == null ? null : entity.getAppId());
        dto.setAppSecretConfigured(entity != null && !isBlank(entity.getAppSecret()));
        dto.setUpdatedBy(entity == null ? null : entity.getUpdatedBy());
        dto.setUpdatedAt(entity == null || entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }

    public FeishuConfigDto save(FeishuConfigDto input, String operator) {
        FeishuConfig entity = repository.findById(CONFIG_ID).orElseGet(() -> {
            FeishuConfig created = new FeishuConfig();
            created.setId(CONFIG_ID);
            return created;
        });
        entity.setApiUrl(isBlank(input.getApiUrl()) ? DEFAULT_API_URL : input.getApiUrl().trim());
        entity.setAppId(trim(input.getAppId()));
        if (!isBlank(input.getAppSecret())) {
            entity.setAppSecret(input.getAppSecret().trim());
        }
        entity.setUpdatedBy(operator);
        repository.save(entity);
        return getUiConfig();
    }

    public Map<String, Object> testConnection(FeishuConfigDto input) {
        FeishuConfig runtime = mergeRuntime(input);
        String token = getTenantAccessToken(runtime);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", token != null && token.length() > 0);
        result.put("tokenLength", token == null ? 0 : token.length());
        result.put("apiUrl", normalizeApiUrl(runtime.getApiUrl()));
        return result;
    }

    public Map<String, Object> sendMessage(FeishuMessageRequest request) {
        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("email is required");
        }
        if (isBlank(request.getText())) {
            throw new IllegalArgumentException("text is required");
        }
        FeishuConfig config = repository.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException("Feishu config is not set"));
        String token = getTenantAccessToken(config);
        String openId = findOpenIdByEmail(config, token, request.getEmail().trim());
        sendTextByOpenId(config, token, openId, request.getText());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", request.getEmail());
        result.put("openIdResolved", true);
        result.put("sent", true);
        return result;
    }

    private String getTenantAccessToken(FeishuConfig config) {
        require(config.getAppId(), "app_id");
        require(config.getAppSecret(), "app_secret");
        Map<String, String> body = new LinkedHashMap<>();
        body.put("app_id", config.getAppId().trim());
        body.put("app_secret", config.getAppSecret().trim());
        JsonNode json = postJson(config, "/open-apis/auth/v3/tenant_access_token/internal", null, body);
        int code = json.path("code").asInt(-1);
        if (code != 0) {
            throw new IllegalStateException("Feishu token failed: " + json.path("msg").asText());
        }
        return json.path("tenant_access_token").asText();
    }

    private String findOpenIdByEmail(FeishuConfig config, String token, String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("emails", Collections.singletonList(email));
        JsonNode json = postJson(config, "/open-apis/contact/v3/users/batch_get_id?user_id_type=open_id", token, body);
        int code = json.path("code").asInt(-1);
        if (code != 0) {
            throw new IllegalStateException("Feishu user lookup failed: " + json.path("msg").asText());
        }
        JsonNode users = json.path("data").path("user_list");
        if (!users.isArray() || users.size() == 0 || isBlank(users.get(0).path("user_id").asText())) {
            throw new IllegalStateException("Feishu user not found: " + email);
        }
        return users.get(0).path("user_id").asText();
    }

    private void sendTextByOpenId(FeishuConfig config, String token, String openId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("receive_id", openId);
        body.put("msg_type", "text");
        try {
            body.put("content", objectMapper.writeValueAsString(Collections.singletonMap("text", text)));
        } catch (Exception e) {
            throw new IllegalStateException("Build Feishu message failed", e);
        }
        JsonNode json = postJson(config, "/open-apis/im/v1/messages?receive_id_type=open_id", token, body);
        int code = json.path("code").asInt(-1);
        if (code != 0) {
            throw new IllegalStateException("Feishu send failed: " + json.path("msg").asText());
        }
    }

    private JsonNode postJson(FeishuConfig config, String path, String bearerToken, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!isBlank(bearerToken)) {
                headers.setBearerAuth(bearerToken);
            }
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeApiUrl(config.getApiUrl()) + path,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class
            );
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Feishu request failed: " + e.getMessage(), e);
        }
    }

    private FeishuConfig mergeRuntime(FeishuConfigDto input) {
        FeishuConfig stored = repository.findById(CONFIG_ID).orElse(null);
        FeishuConfig runtime = new FeishuConfig();
        runtime.setApiUrl(!isBlank(input.getApiUrl()) ? input.getApiUrl() : stored == null ? DEFAULT_API_URL : stored.getApiUrl());
        runtime.setAppId(!isBlank(input.getAppId()) ? input.getAppId() : stored == null ? null : stored.getAppId());
        runtime.setAppSecret(!isBlank(input.getAppSecret()) ? input.getAppSecret() : stored == null ? null : stored.getAppSecret());
        return runtime;
    }

    private String normalizeApiUrl(String value) {
        String url = isBlank(value) ? DEFAULT_API_URL : value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private void require(String value, String name) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
