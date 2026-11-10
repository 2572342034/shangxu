package com.shangxu.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "crm.jwt")
public class JwtProperties {

    private String secret = "商序云@2026#SecretKey$ForJwtTokenGeneration!MustBeLongEnough";
    private long expire = 28800;
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
