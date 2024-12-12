package com.certapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;

@Configuration
public class FileIOConfig {
    @Bean
    public ExecutorService fileIOExecutor() {
        return Executors.newFixedThreadPool(20); // 文件IO线程池
    }
} 