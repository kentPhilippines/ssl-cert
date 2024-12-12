package com.certapp.repository;

import com.certapp.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    // 可以添加自定义查询方法
} 