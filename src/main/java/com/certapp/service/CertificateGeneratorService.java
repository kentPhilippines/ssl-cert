package com.certapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import java.math.BigInteger;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CertificateGeneratorService {
    
    public byte[] generateCertificate(String commonName, String organization) throws Exception {
        // 生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 证书生成器
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=" + commonName + ", O=" + organization);

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName); // 自签名
        certGen.setNotBefore(new Date(System.currentTimeMillis()));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        X509Certificate cert = certGen.generate(keyPair.getPrivate());
        return cert.getEncoded();
    }
} 