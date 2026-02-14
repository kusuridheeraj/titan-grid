package com.titan.cryptex.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultTransitContext;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;

/**
 * Core Streaming Encryption Service.
 * Implements "Envelope Encryption" pattern using HashiCorp Vault.
 * 
 * Demonstrates: SDE-3 Competency (Memory Management & Cryptography).
 * Encrypts files on-the-fly without loading them into RAM.
 * 
 * Format: [IV (12 bytes)] [Encrypted DEK Length (4 bytes)] [Encrypted DEK (...)] [File Content ...]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingEncryptionService {

    private final VaultOperations vaultOperations;

    private static final String TRANSIT_KEY_NAME = "titan-grid-key";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 32; // 256 bits

    /**
     * Encrypts an InputStream on-the-fly using AES-256-GCM.
     * Generates a Data Encryption Key (DEK), encrypts it via Vault, prepends it to the stream.
     * 
     * @param inputStream The raw file stream
     * @param context     Context for key derivation (e.g., user ID, file ID)
     * @return EncryptedInputStream wrapper (Self-contained blob)
     */
    @SneakyThrows
    public InputStream encryptStream(InputStream inputStream, String context) {
        log.info("Starting encryption pipeline for context: {}", context);

        // 1. Generate a random IV (Initialization Vector)
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 2. Generate a random Data Encryption Key (DEK) - 256 bit
        byte[] dek = new byte[AES_KEY_SIZE];
        new SecureRandom().nextBytes(dek);
        SecretKeySpec secretKey = new SecretKeySpec(dek, "AES");

        // 3. Encrypt the DEK using Vault (Transit Engine)
        // This is the "Envelope Encryption" step. Ideally, Vault encrypts the key for us.
        String encryptedDek = vaultOperations.opsForTransit()
                .encrypt(TRANSIT_KEY_NAME, dek, VaultTransitContext.fromContext(context.getBytes()));
        
        byte[] encryptedDekBytes = encryptedDek.getBytes();
        int encryptedDekLength = encryptedDekBytes.length;

        // 4. Create the Header
        // [IV (12)] [Length (4)] [Encrypted DEK (...)]
        ByteBuffer headerBuffer = ByteBuffer.allocate(GCM_IV_LENGTH + 4 + encryptedDekLength);
        headerBuffer.put(iv);
        headerBuffer.putInt(encryptedDekLength);
        headerBuffer.put(encryptedDekBytes);
        byte[] header = headerBuffer.array();

        // 5. Initialize Cipher (AES-GCM) with Plaintext DEK
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        // 6. Chain the Streams: Header + Encrypted Content
        InputStream headerStream = new ByteArrayInputStream(header);
        InputStream contentStream = new CipherInputStream(inputStream, cipher);
        
        // Return combined stream
        return new SequenceInputStream(Collections.enumeration(java.util.Arrays.asList(headerStream, contentStream)));
    }

    /**
     * Decrypts an InputStream on-the-fly.
     * Reads the header, decrypts the DEK via Vault, and streams the plaintext.
     * 
     * @param inputStream The encrypted stream (Self-contained blob)
     * @param context     Context used during encryption
     * @return Decrypted InputStream
     */
    @SneakyThrows
    public InputStream decryptStream(InputStream inputStream, String context) {
        log.info("Starting decryption pipeline for context: {}", context);

        // 1. Read IV (12 bytes)
        byte[] iv = new byte[GCM_IV_LENGTH];
        if (inputStream.read(iv) != GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Stream too short: Missing IV");
        }

        // 2. Read Encrypted DEK Length (4 bytes)
        byte[] lengthBytes = new byte[4];
        if (inputStream.read(lengthBytes) != 4) {
            throw new IllegalArgumentException("Stream too short: Missing DEK Length");
        }
        int encryptedDekLength = ByteBuffer.wrap(lengthBytes).getInt();

        // 3. Read Encrypted DEK
        byte[] encryptedDekBytes = new byte[encryptedDekLength];
        if (inputStream.read(encryptedDekBytes) != encryptedDekLength) {
            throw new IllegalArgumentException("Stream too short: Missing Encrypted DEK");
        }
        String encryptedDek = new String(encryptedDekBytes);

        // 4. Decrypt DEK using Vault
        byte[] dek = vaultOperations.opsForTransit()
                .decrypt(TRANSIT_KEY_NAME, encryptedDek, VaultTransitContext.fromContext(context.getBytes()));
        SecretKeySpec secretKey = new SecretKeySpec(dek, "AES");

        // 5. Initialize Cipher (AES-GCM) for Decryption
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        // 6. Return Decrypted Stream
        return new CipherInputStream(inputStream, cipher);
    }
}
