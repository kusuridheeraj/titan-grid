package com.titan.cryptex.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Storage Client Configuration.
 * Configures either MinIO or AWS S3 client based on properties.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    @Value("${cryptex.storage.minio.endpoint}")
    private String endpoint;

    @Value("${cryptex.storage.minio.access-key}")
    private String accessKey;

    @Value("${cryptex.storage.minio.secret-key}")
    private String secretKey;

    @Value("${cryptex.storage.minio.region:us-east-1}")
    private String region;

    @Bean
    @ConditionalOnProperty(name = "cryptex.storage.provider", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient() {
        log.info("Connecting to MinIO at: {}", endpoint);
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "cryptex.storage.provider", havingValue = "s3")
    public AmazonS3 s3Client() {
        log.info("Connecting to AWS S3 (Region: {})", region);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(region)
                .build();
    }
}
