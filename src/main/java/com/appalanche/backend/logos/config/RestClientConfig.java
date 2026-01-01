package com.appalanche.backend.logos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Value("${integration.logo-dev.base-url}")
    private String baseUrl;

    @Value("${integration.logo-dev.connect-timeout}")
    private int connectTimeout;

    @Value("${integration.logo-dev.read-timeout}")
    private int readTimeout;

    @Bean("logoClient")
    public RestClient logoClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return builder.baseUrl(baseUrl)
                      .requestFactory(factory)
                      .build();
    }
}
