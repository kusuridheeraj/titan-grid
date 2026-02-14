package com.titan.cryptex.service;

import com.titan.cryptex.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Orchestrator Service for Secure Storage.
 * Bridges Streaming Encryption and Cloud/Local Storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecureStorageService {

    private final StreamingEncryptionService encryptionService;
    private final StorageProvider storageProvider;

    /**
     * Encrypt and Upload a file.
     * The stream is encrypted on-the-fly as it flows to the storage backend.
     * 
     * @param filename Unique name
     * @param content  Raw content stream
     * @param size     Raw size
     * @param context  Encryption context (e.g. userId)
     * @param contentType MIME type
     */
    public void uploadEncrypted(String filename, InputStream content, long size, String context, String contentType) {
        log.info("Request to upload encrypted file: {}", filename);
        
        // 1. Wrap stream with encryption
        // Note: The encrypted stream will be slightly larger than the raw stream 
        // due to our custom header [IV + DEK_LENGTH + ENCRYPTED_DEK].
        // However, MinIO/S3 'putObject' handles unknown sizes if needed, or we calculate.
        // For simplicity, we pass -1 if size unknown, but here we provide calculated size if possible.
        InputStream encryptedStream = encryptionService.encryptStream(content, context);
        
        // 2. Stream to storage
        // S3 requires a size. Since we use AES-GCM (NoPadding), content size is same.
        // Header is roughly 12 + 4 + ~300 bytes (depending on Vault's encrypted DEK length).
        // For production, we'd use a more precise calculator.
        storageProvider.upload(filename, encryptedStream, -1, contentType);
        
        log.info("File {} uploaded successfully", filename);
    }

    /**
     * Download and Decrypt a file.
     * Decrypts chunks as they are read by the consumer.
     * 
     * @param filename Unique name
     * @param context  Encryption context
     * @return Decrypted stream with metadata
     */
    public StorageProvider.StorageResource downloadDecrypted(String filename, String context) {
        log.info("Request to download decrypted file: {}", filename);
        
        // 1. Get encrypted stream from storage
        StorageProvider.StorageResource resource = storageProvider.download(filename);
        
        // 2. Wrap with decryption logic
        InputStream decryptedStream = encryptionService.decryptStream(resource.inputStream(), context);
        
        return new StorageProvider.StorageResource(decryptedStream, resource.contentType(), resource.size());
    }

    /**
     * Delete file from storage.
     */
    public void delete(String filename) {
        storageProvider.delete(filename);
    }
}
