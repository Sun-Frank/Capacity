package com.capics.dto;

import java.util.List;

public class LoginResponse {
    private String token;
    private String username;
    private String realName;
    private Long id;
    private List<String> roleCodes;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, String realName, Long id) {
        this.token = token;
        this.username = username;
        this.realName = realName;
        this.id = id;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return this.realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getRoleCodes() {
        return this.roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
