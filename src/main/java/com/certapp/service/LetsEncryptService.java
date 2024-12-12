package com.certapp.service;

import lombok.RequiredArgsConstructor;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import java.io.*;
import java.security.KeyPair;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import com.certapp.config.DynamicSSLConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetsEncryptService {
    private static final String LETS_ENCRYPT_URL = "acme://letsencrypt.org";
    private static final File USER_KEY_FILE = new File("user.key");
    private static final File DOMAIN_KEY_FILE = new File("domain.key");
    private static final File DOMAIN_CERT_FILE = new File("domain.crt");
    
    private final DynamicSSLConfig dynamicSSLConfig;
    
    public void requestCertificate(String domain) throws Exception {
        log.info("开始申请Let's Encrypt证书: {}", domain);
        
        KeyPair userKeyPair = loadOrCreateUserKeyPair();
        log.debug("用户密钥对加载完成");
        
        Session session = new Session(LETS_ENCRYPT_URL);
        Account account = createOrLoadAccount(session, userKeyPair);
        log.debug("ACME账户创建/加载完成");
        
        // 3. 创建域名密钥对
        KeyPair domainKeyPair = loadOrCreateDomainKeyPair();
        
        // 4. 创建证书订单
        Order order = account.newOrder()
            .domains(domain)
            .create();
        
        // 5. 处理域名验证挑战
        for (Authorization auth : order.getAuthorizations()) {
            processHttpChallenge(auth);
        }
        
        // 6. 生成CSR并完成订单
        CSRBuilder csrBuilder = new CSRBuilder();
        csrBuilder.addDomain(domain);
        csrBuilder.sign(domainKeyPair);
        
        order.execute(csrBuilder.getEncoded());
        
        // 7. 等待订单完成
        while (order.getStatus() != Status.VALID) {
            Thread.sleep(3000L);
            order.update();
        }
        
        // 8. 获取证书并保存
        Certificate certificate = order.getCertificate();
        try (FileWriter fw = new FileWriter(DOMAIN_CERT_FILE)) {
            certificate.writeCertificate(fw);
        }
        
        // 导入到密钥库
        importCertificateToKeystore(certificate, domainKeyPair);
        
        log.info("证书申请完成: {}", domain);
    }
    
    private KeyPair loadOrCreateUserKeyPair() throws IOException {
        if (USER_KEY_FILE.exists()) {
            try (FileReader fr = new FileReader(USER_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
            try (FileWriter fw = new FileWriter(USER_KEY_FILE)) {
                KeyPairUtils.writeKeyPair(keyPair, fw);
            }
            return keyPair;
        }
    }
    
    private KeyPair loadOrCreateDomainKeyPair() throws IOException {
        if (DOMAIN_KEY_FILE.exists()) {
            try (FileReader fr = new FileReader(DOMAIN_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
            try (FileWriter fw = new FileWriter(DOMAIN_KEY_FILE)) {
                KeyPairUtils.writeKeyPair(keyPair, fw);
            }
            return keyPair;
        }
    }
    
    private Account createOrLoadAccount(Session session, KeyPair keyPair) throws AcmeException {
        AccountBuilder accountBuilder = new AccountBuilder()
            .agreeToTermsOfService()
            .useKeyPair(keyPair);
        return accountBuilder.create(session);
    }
    
    private void processHttpChallenge(Authorization auth) throws Exception {
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
        if (challenge == null) {
            log.error("找不到HTTP验证挑战");
            throw new Exception("无法找到HTTP验证挑战");
        }
        
        log.debug("开始处理域名验证挑战: token={}", challenge.getToken());
        
        // 这里需要实现将challenge token和authorization放到web服务器指定径的逻辑
        // Let's Encrypt将访问 http://<domain>/.well-known/acme-challenge/<token>
        // 期望获得authorization
        String token = challenge.getToken();
        String authorization = challenge.getAuthorization();
        
        // TODO: 实现保存challenge响应的逻辑
        
        // 触发验证
        challenge.trigger();
        
        // 等待验证完成
        while (challenge.getStatus() != Status.VALID) {
            Thread.sleep(3000L);
            challenge.update();
        }
    }
    
    private void importCertificateToKeystore(Certificate certificate, KeyPair domainKeyPair) throws Exception {
        // 创建PKCS12密钥库
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        // 获取证书链
        X509Certificate[] chain = certificate.getCertificateChain()
            .stream()
            .map(cert -> (X509Certificate) cert)
            .toArray(X509Certificate[]::new);
        
        // 导入私钥和证书链
        keyStore.setKeyEntry("tomcat", 
            domainKeyPair.getPrivate(),
            "changeit".toCharArray(),
            chain);
        
        // 保存密钥库到配置的路径
        File keystoreFile = new File("domain.key");
        try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
            keyStore.store(fos, "changeit".toCharArray());
        }
        
        log.info("证书已导入到密钥库: {}", keystoreFile.getAbsolutePath());
        
        // 动态启用HTTPS
        dynamicSSLConfig.enableSSL(keystoreFile.getAbsolutePath(), "changeit");
    }
} 