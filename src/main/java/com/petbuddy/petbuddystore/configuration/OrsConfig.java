package com.petbuddy.petbuddystore.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Data
public class OrsConfig {
    @Value("${ors.api.key}")
    private String apiKey;

    @Value("${ors.api.base-url}")
    private String baseUrl;

    @Bean(name = "orsRestTemplate")
    public RestTemplate orsRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
