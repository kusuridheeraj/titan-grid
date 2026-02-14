package com.titan.cryptex.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO Implementation of StorageProvider.
 * MinIO is S3-compatible, so this logic works for AWS S3 as well.
 * 
 * Uses streaming upload (PutObject) to handle large files with constant memory usage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cryptex.storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioStorageProvider implements StorageProvider {

    private final MinioClient minioClient;

    @Value("${cryptex.storage.bucket-name}")
    private String bucketName;

    /**
     * Initialize the bucket on startup if it doesn't exist.
     */
    @PostConstruct
    @SneakyThrows
    public void init() {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            log.info("Bucket '{}' not found. Creating it...", bucketName);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } else {
            log.info("Bucket '{}' already exists.", bucketName);
        }
    }

    @Override
    @SneakyThrows
    public void upload(String filename, InputStream content, long size, String contentType) {
        log.info("Starting streaming upload for: {} (Size: {} bytes)", filename, size);

        // PutObject streams data directly from InputStream to S3/MinIO
        // Part size: -1 (auto-detect multipart upload)
        // Backpressure is handled by the network socket
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .stream(content, size, -1) 
                        .contentType(contentType)
                        .build()
        );

        log.info("Upload complete: {}", filename);
    }

    @Override
    @SneakyThrows
    public StorageResource download(String filename) {
        log.info("Starting streaming download for: {}", filename);
        
        GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .build()
        );
        
        return new StorageResource(
                response, 
                response.headers().get("Content-Type"),
                -1 // Size is streamed
        );
    }

    @Override
    @SneakyThrows
    public void delete(String filename) {
        log.info("Deleting file: {}", filename);
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .build()
        );
    }
}
