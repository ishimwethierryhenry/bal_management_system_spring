package com.bal.bal_management_system.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor())
                .excludePathPatterns("/", "/login", "/register", "/logout",
                        "/aboutBal", "/matches", "/standings",
                        "/stats", "/players", "/gallery",
                        "/css/**", "/js/**", "/images/**", "/uploads/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Match all endpoints
                .allowedOrigins("http://localhost:3000") // Allow frontend origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Methods to allow
                .allowedHeaders("Authorization", "Content-Type", "Cache-Control") // Allowed headers
                .allowCredentials(true) // Allow cookies/session sharing
                .maxAge(3600); // Cache preflight response for 1 hour
    }

    // Inner class: AuthenticationInterceptor
    private static class AuthenticationInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            String requestMethod = request.getMethod();

            // Allow OPTIONS requests to bypass authentication checks
            if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
                return true; // Allow preflight requests to proceed
            }

            HttpSession session = request.getSession(false);
            String requestURI = request.getRequestURI();

            // Check if user is authenticated
            if (session == null || session.getAttribute("loggedInUser") == null) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                response.sendRedirect("/login");
                return false;
            }

            // Check admin access for protected endpoints
            if (requestURI.startsWith("/admin")) {
                String role = (String) session.getAttribute("role");
                if (!"ADMIN".equalsIgnoreCase(role)) {
                    response.sendRedirect("/access-denied");
                    return false;
                }
            }

            return true; // Proceed to the next handler
        }
    }
}
