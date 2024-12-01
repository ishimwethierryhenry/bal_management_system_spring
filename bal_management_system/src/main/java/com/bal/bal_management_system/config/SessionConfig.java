package com.bal.bal_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.http.HttpSessionListener;

@Configuration
public class SessionConfig implements WebMvcConfigurer {
    @Bean
    public HttpSessionListener httpSessionListener() {
        return new CustomSessionListener();
    }

    // Custom Session Listener
    public static class CustomSessionListener implements HttpSessionListener {
        @Override
        public void sessionCreated(jakarta.servlet.http.HttpSessionEvent se) {
            se.getSession().setMaxInactiveInterval(1800); // 30 minutes
        }
    }
}