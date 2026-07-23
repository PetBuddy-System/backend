package com.petbuddy.petbuddystore.configuration;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "momo")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MomoConfig {
    String endpoint;
    String accessKey;
    String partnerCode;
    String secretKey;
    String redirectUrl;
    String ipnUrl;
    String queryUrl;
}
