package com.esprit.microservice.apicamping.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {

    private String secretKey;
    private String publishableKey;
    private String webhookSecret;
    private String currency = "usd";
    private String frontendBaseUrl = "http://localhost:4200";
}
