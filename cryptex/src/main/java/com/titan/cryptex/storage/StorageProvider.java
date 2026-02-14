package com.titan.cryptex.storage;

import java.io.InputStream;

/**
 * Interface for abstracting different storage backends.
 */
public interface StorageProvider {

    void upload(String filename, InputStream content, long size, String contentType);

    /**
     * Download a resource including its metadata.
     */
    StorageResource download(String filename);

    void delete(String filename);

    /**
     * Internal record to hold stream and metadata.
     */
    record StorageResource(InputStream inputStream, String contentType, long size) {}
}
