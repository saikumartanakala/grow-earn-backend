package com.growearn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Base64;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Delete an image from Cloudinary using its public_id
     * @param publicId The public_id of the image to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            logger.warn("Cannot delete image: publicId is null or empty");
            return false;
        }

        if (cloudName == null || cloudName.isEmpty() || 
            apiKey == null || apiKey.isEmpty() || 
            apiSecret == null || apiSecret.isEmpty()) {
            logger.warn("Cloudinary credentials not configured. Skipping image deletion for publicId: {}", publicId);
            return false;
        }

        try {
            String url = String.format("https://api.cloudinary.com/v1_1/%s/image/destroy", cloudName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create basic auth header
            String auth = apiKey + ":" + apiSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            Map<String, String> requestBody = Map.of("public_id", publicId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String result = (String) response.getBody().get("result");
                if ("ok".equals(result)) {
                    logger.info("Successfully deleted image from Cloudinary: {}", publicId);
                    return true;
                } else {
                    logger.warn("Cloudinary deletion returned non-ok result: {} for publicId: {}", result, publicId);
                    return false;
                }
            } else {
                logger.error("Failed to delete image from Cloudinary. Status: {}, publicId: {}", 
                           response.getStatusCode(), publicId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting image from Cloudinary for publicId: {}", publicId, e);
            return false;
        }
    }

    /**
     * Delete multiple images in batch
     * @param publicIds Array of public_ids to delete
     * @return Number of successfully deleted images
     */
    public int deleteBatch(String[] publicIds) {
        int deletedCount = 0;
        for (String publicId : publicIds) {
            if (deleteImage(publicId)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }
}
