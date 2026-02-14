package com.titan.cryptex.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * AWS S3 Implementation of StorageProvider.
 * Active when cryptex.storage.provider=s3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cryptex.storage.provider", havingValue = "s3")
public class S3StorageProvider implements StorageProvider {

    private final AmazonS3 s3Client;

    @Value("${cryptex.storage.bucket-name}")
    private String bucketName;

    @Override
    public void upload(String filename, InputStream content, long size, String contentType) {
        log.info("Starting S3 upload for: {} (Size: {} bytes)", filename, size);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);

        s3Client.putObject(new PutObjectRequest(bucketName, filename, content, metadata));
        log.info("S3 upload complete: {}", filename);
    }

    @Override
    public InputStream download(String filename) {
        log.info("Starting S3 download for: {}", filename);
        S3Object object = s3Client.getObject(bucketName, filename);
        return object.getObjectContent();
    }

    @Override
    public void delete(String filename) {
        log.info("Deleting file from S3: {}", filename);
        s3Client.deleteObject(bucketName, filename);
    }
}
