package com.titan.cryptex.controller;

import com.titan.cryptex.service.SecureStorageService;
import com.titan.cryptex.storage.StorageProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

/**
 * REST Controller for Zero-Trust Storage.
 * Demonstrates: Non-blocking I/O and Constant Memory Footprint.
 */
@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Secure Streaming Storage API")
public class StorageController {

    private final SecureStorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and Encrypt", description = "Streams file to Vault/Storage without loading into RAM.")
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Titan-Context", defaultValue = "default-user") String context) {
        
        // Staff Strategy: Use try-with-resources to ensure input stream closure
        try (InputStream inputStream = file.getInputStream()) {
            log.info("Receiving upload: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
            
            storageService.uploadEncrypted(
                    file.getOriginalFilename(), 
                    inputStream, 
                    file.getSize(), 
                    context, 
                    file.getContentType()
            );
            
            return ResponseEntity.ok("File uploaded and encrypted successfully.");
            
        } catch (Exception e) {
            log.error("Upload failed for file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body("Error during upload: " + e.getMessage());
        }
    }

    @GetMapping("/download/{filename}")
    @Operation(summary = "Download and Decrypt", description = "Decrypts file on-the-fly via streaming.")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable String filename,
            @RequestHeader(value = "X-Titan-Context", defaultValue = "default-user") String context) {
        
        try {
            log.info("Requesting download: {}", filename);
            
            StorageProvider.StorageResource resource = storageService.downloadDecrypted(filename, context);
            
            // Staff Strategy: Set the original content type instead of binary octet-stream
            String contentType = resource.contentType() != null ? resource.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            StreamingResponseBody responseBody = outputStream -> {
                // Buffer size 8KB is standard for streaming IO
                try (InputStream decryptedStream = resource.inputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = decryptedStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (Exception e) {
                    log.error("Stream interrupted during download of {}", filename, e);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(responseBody);

        } catch (Exception e) {
            log.error("Download initialization failed for {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Void> delete(@PathVariable String filename) {
        storageService.delete(filename);
        return ResponseEntity.noContent().build();
    }
}
