package com.certapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class FileService {
    private final ExecutorService fileIOExecutor;
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB缓冲区

    public Future<byte[]> readFileAsync(String filePath) {
        return fileIOExecutor.submit(() -> {
            try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    Path.of(filePath), StandardOpenOption.READ)) {
                
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                byte[] content = new byte[(int) channel.size()];
                int position = 0;
                
                while (position < channel.size()) {
                    Future<Integer> future = channel.read(buffer, position);
                    int bytesRead = future.get();
                    
                    if (bytesRead == -1) break;
                    
                    buffer.flip();
                    buffer.get(content, position, bytesRead);
                    buffer.clear();
                    position += bytesRead;
                }
                
                return content;
            }
        });
    }
} 