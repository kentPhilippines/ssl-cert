package com.certapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class SSLContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {
    
    @Value("${server.ssl.key-store}")
    private String keystorePath;
    
    @Value("${server.ssl.key-store-password}")
    private String keystorePassword;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            refreshSSLContext();
            log.info("SSL上下文已更新");
        } catch (Exception e) {
            log.error("更新SSL上下文失败", e);
        }
    }
    
    private void refreshSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);
    }
} 