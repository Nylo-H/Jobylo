package com.example.TestAPI.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private BigDecimal commissionPercentage = BigDecimal.valueOf(5);
    private String defaultMethod = "DEMO";
}
