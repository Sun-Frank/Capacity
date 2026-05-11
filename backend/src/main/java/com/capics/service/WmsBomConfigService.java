package com.capics.service;

import com.capics.dto.WmsBomConfigDto;
import com.capics.entity.WmsBomConfig;
import com.capics.repository.WmsBomConfigRepository;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WmsBomConfigService {
    private static final int CONFIG_ID = 1;

    private final WmsBomConfigRepository repository;

    public WmsBomConfigService(WmsBomConfigRepository repository) {
        this.repository = repository;
    }

    public WmsBomConfigDto getUiConfig() {
        WmsBomConfig entity = repository.findById(CONFIG_ID).orElse(null);
        WmsBomConfigDto dto = new WmsBomConfigDto();
        if (entity == null) {
            return dto;
        }
        dto.setLoginUrl(entity.getLoginUrl());
        dto.setUsername(entity.getUsername());
        dto.setPassword("");
        dto.setPasswordConfigured(!isBlank(entity.getPassword()));
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }

    public WmsBomConfigDto save(WmsBomConfigDto input, String operator) {
        WmsBomConfig entity = repository.findById(CONFIG_ID).orElseGet(() -> {
            WmsBomConfig created = new WmsBomConfig();
            created.setId(CONFIG_ID);
            return created;
        });

        entity.setLoginUrl(trimToNull(input.getLoginUrl()));
        entity.setUsername(trimToNull(input.getUsername()));
        if (input.getPassword() != null && !input.getPassword().isEmpty()) {
            entity.setPassword(input.getPassword());
        }
        entity.setUpdatedBy(operator);
        repository.save(entity);
        return getUiConfig();
    }

    public WmsBomConfig getRuntimeConfig() {
        WmsBomConfig config = repository.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException("WMS BOM config is not saved"));
        if (isBlank(config.getLoginUrl()) || isBlank(config.getUsername()) || isBlank(config.getPassword())) {
            throw new IllegalStateException("WMS URL, username and password are required");
        }
        return config;
    }

    public Map<String, Object> testConnection(WmsBomConfigDto input) {
        WmsBomConfig runtime = mergeRuntime(input);
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        result.put("endpoint", runtime.getLoginUrl());
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(runtime.getLoginUrl()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            int status = connection.getResponseCode();
            result.put("ok", status >= 200 && status < 500);
            result.put("status", status);
            result.put("message", status >= 200 && status < 500 ? "WMS URL is reachable" : "WMS URL returned HTTP " + status);
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("message", ex.getMessage());
        } finally {
            result.put("latencyMs", System.currentTimeMillis() - start);
        }
        return result;
    }

    private WmsBomConfig mergeRuntime(WmsBomConfigDto input) {
        WmsBomConfig stored = repository.findById(CONFIG_ID).orElse(null);
        WmsBomConfig runtime = new WmsBomConfig();
        runtime.setLoginUrl(firstNonBlank(input.getLoginUrl(), stored == null ? null : stored.getLoginUrl()));
        runtime.setUsername(firstNonBlank(input.getUsername(), stored == null ? null : stored.getUsername()));
        runtime.setPassword(firstNonBlank(input.getPassword(), stored == null ? null : stored.getPassword()));
        if (isBlank(runtime.getLoginUrl())) {
            throw new IllegalArgumentException("WMS URL is required");
        }
        return runtime;
    }

    private String firstNonBlank(String first, String fallback) {
        String normalized = trimToNull(first);
        return normalized == null ? trimToNull(fallback) : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
