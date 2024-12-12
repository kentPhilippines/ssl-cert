package com.certapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class HttpRedirectConfig implements WebMvcConfigurer {
    
    @Bean
    public HttpsRedirectInterceptor httpsRedirectInterceptor() {
        return new HttpsRedirectInterceptor();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpsRedirectInterceptor());
    }
    
    private static class HttpsRedirectInterceptor extends HandlerInterceptorAdapter {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (!request.isSecure() && !"localhost".equals(request.getServerName())) {
                String httpsUrl = "https://" + request.getServerName() + 
                    (request.getServerPort() != 80 ? ":" + request.getServerPort() : "") +
                    request.getRequestURI();
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", httpsUrl);
                return false;
            }
            return true;
        }
    }
} 