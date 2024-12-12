package com.certapp.controller;

import com.certapp.model.Certificate;
import com.certapp.service.CertificateService;
import com.certapp.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService certificateService;
    private final FileService fileService;
    
    @GetMapping
    public ResponseEntity<List<Certificate>> getList() {
        log.debug("获取证书列表");
        return ResponseEntity.ok(certificateService.getList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getById(@PathVariable Long id) {
        return ResponseEntity.ok(certificateService.getById(id));
    }
    
    @PostMapping
    public ResponseEntity<Certificate> apply(@RequestBody Certificate certificate) {
        return ResponseEntity.ok(certificateService.apply(certificate));
    }
    
    @GetMapping("/file/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        try {
            Future<byte[]> future = fileService.readFileAsync("path/to/files/" + filename);
            byte[] content = future.get(5, TimeUnit.SECONDS); // 设置超时时间
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/one-click")
    public ResponseEntity<Certificate> oneClickApply(@RequestBody Certificate request) {
        log.info("收到证书申请请求: domain={}, type={}", request.getName(), request.getType());
        try {
            Certificate result = certificateService.oneClickApply(request);
            log.info("证书申请成功: id={}", result.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("证书申请失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable Long id) {
        try {
            Certificate cert = certificateService.getById(id);
            String fileName = cert.getDescription()
                .lines()
                .filter(line -> line.startsWith("Certificate file:"))
                .findFirst()
                .map(line -> line.substring("Certificate file:".length()).trim())
                .orElseThrow();

            Path certPath = Path.of("certificates", fileName);
            Resource resource = new FileSystemResource(certPath.toFile());

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
} 