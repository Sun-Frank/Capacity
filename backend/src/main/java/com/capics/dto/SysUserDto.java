package com.capics.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUserDto {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private Boolean enabled;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
