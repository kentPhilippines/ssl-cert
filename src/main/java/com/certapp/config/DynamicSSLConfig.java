package com.certapp.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

@Slf4j
@Component
public class DynamicSSLConfig {
    
    @Autowired
    private ServletWebServerApplicationContext applicationContext;
    
    public void enableSSL(String keystorePath, String keystorePassword) {
        try {
            // 加载密钥库
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, keystorePassword.toCharArray());
            }
            
            // 创建SSL上下文
            SSLContext sslContext = createSSLContext(keyStore, keystorePassword);
            
            // 获取Tomcat服务器
            TomcatWebServer tomcatWebServer = (TomcatWebServer) applicationContext.getWebServer();
            org.apache.catalina.Server server = tomcatWebServer.getTomcat().getServer();
            
            // 为每个服务添加HTTPS连接器
            for (org.apache.catalina.Service service : server.findServices()) {
                // 创建新的HTTPS连接器
                Connector connector = new Connector("HTTP/1.1");
                connector.setScheme("https");
                connector.setSecure(true);
                connector.setPort(443); // HTTPS标准端口
                
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                protocol.setSSLEnabled(true);
                protocol.setKeystoreFile(keystorePath);
                protocol.setKeystorePass(keystorePassword);
                protocol.setKeystoreType("PKCS12");
                
                // 添加新连接器
                service.addConnector(connector);
            }
            
            log.info("HTTPS已动态启用，端口: 443");
        } catch (Exception e) {
            log.error("启用HTTPS失败", e);
            throw new RuntimeException("无法启用HTTPS", e);
        }
    }
    
    private SSLContext createSSLContext(KeyStore keyStore, String keystorePassword) throws Exception {
        // 创建密钥管理器
        javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
            .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());
        
        // 创建信任管理器
        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
            .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        
        // 创建并初始化SSL上下文
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        
        return sslContext;
    }
} 