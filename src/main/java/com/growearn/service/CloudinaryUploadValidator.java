package com.growearn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class CloudinaryUploadValidator {

    @Value("${cloudinary.upload.max-bytes:5242880}")
    private long maxBytes;

    @Value("${cloudinary.upload.allowed-mime:image/jpeg,image/png,image/webp}")
    private String allowedMimeRaw;

    public void validate(String mimeType, Long sizeBytes) {
        if (mimeType == null || sizeBytes == null) {
            return;
        }
        if (sizeBytes <= 0 || sizeBytes > maxBytes) {
            throw new IllegalArgumentException("Invalid proof upload size");
        }
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedMimeRaw.split(",")));
        if (!allowed.contains(mimeType)) {
            throw new IllegalArgumentException("Invalid proof upload type");
        }
    }
}
