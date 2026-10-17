package com.pulsepass;

import com.pulsepass.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class PulsepassCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(PulsepassCoreApplication.class, args);
    }
}