package com.wavelength.common;

import java.net.URI;
import java.time.Duration;

// S3/R2 boundary only. Keys are internal opaque IDs, never unchecked client filenames.
public interface ObjectStorage {
    URI createUploadUrl(String key, String contentType, Duration expiresIn);

    URI createDownloadUrl(String key, Duration expiresIn);

    void delete(String key);
}
