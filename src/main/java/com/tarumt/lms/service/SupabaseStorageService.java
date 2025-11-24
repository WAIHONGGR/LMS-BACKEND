package com.tarumt.lms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Upload base64 (digital signature)
    public String uploadBase64(String base64Data, String bucket, String folder, String filename, String mimeType) {
        byte[] data = Base64.getDecoder().decode(base64Data.split(",")[1]);
        return uploadBytes(data, bucket, folder, filename, mimeType);
    }


    // Upload MultipartFile (certificate)
    public String uploadFile(MultipartFile file, String bucket, String folder, String filename) {
        try {
            // Use the actual content type of the file
            return uploadBytes(file.getBytes(), bucket, folder, filename, file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }


    private String uploadBytes(byte[] data, String bucket, String folder, String filename, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseKey);
        headers.setContentType(MediaType.parseMediaType(mimeType)); // <-- use correct MIME type

        HttpEntity<byte[]> request = new HttpEntity<>(data, headers);

        String path = (folder != null && !folder.isBlank()) ? folder + "/" + filename : filename;
        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
        } else {
            throw new RuntimeException("Supabase upload failed: " + response.getBody());
        }
    }


    /**
     * Generate a signed URL for a private file in Supabase Storage
     *
     * @param fileUrl The full public URL of the file (e.g., https://xxx.supabase.co/storage/v1/object/public/bucket/path/file.jpg)
     * @param bucket The bucket name
     * @param expiresInSeconds Number of seconds until the URL expires (default: 3600 = 1 hour)
     * @return Signed URL that can be used to access the private file temporarily
     */
    public String generateSignedUrl(String fileUrl, String bucket, int expiresInSeconds) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        try {
            // Extract the file path from the full URL
            String filePath = extractPathFromUrl(fileUrl, bucket);
            if (filePath == null) {
                log.warn("Could not extract path from URL: {}", fileUrl);
                return fileUrl; // Return original URL as fallback
            }

            // Prepare request to Supabase Storage API
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(supabaseKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Request body for signed URL
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("expiresIn", expiresInSeconds);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Supabase Storage API endpoint for creating signed URLs
            String apiUrl = supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + filePath;

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse the response to get the signed URL
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String signedToken = jsonResponse.has("signedURL")
                        ? jsonResponse.get("signedURL").asText()
                        : null;

                if (signedToken != null) {
                    // Construct the full signed URL
                    String signedUrl = supabaseUrl + "/storage/v1" + signedToken;
                    log.debug("Generated signed URL for: {}", filePath);
                    return signedUrl;
                } else {
                    log.warn("Signed URL not found in response: {}", response.getBody());
                    return fileUrl; // Return original URL as fallback
                }
            } else {
                log.error("Failed to generate signed URL. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return fileUrl; // Return original URL as fallback
            }

        } catch (Exception e) {
            log.error("Error generating signed URL for: {}", fileUrl, e);
            return fileUrl; // Return original URL as fallback
        }
    }


    /**
     * Generate signed URL with default 1 hour expiry
     */
    public String generateSignedUrl(String fileUrl, String bucket) {
        return generateSignedUrl(fileUrl, bucket, 3600); // Default: 1 hour
    }


    /**
     * Extract the file path from a Supabase Storage URL
     *
     * @param fileUrl Full URL (e.g., https://xxx.supabase.co/storage/v1/object/public/bucket/folder/file.jpg)
     * @param bucket The bucket name
     * @return File path (e.g., folder/file.jpg)
     */
    private String extractPathFromUrl(String fileUrl, String bucket) {
        try {
            // Pattern: /storage/v1/object/public/{bucket}/{path}
            // or: /storage/v1/object/{bucket}/{path}
            String pattern = "/storage/v1/object/[^/]+/" + bucket + "/";

            int index = fileUrl.indexOf(pattern);
            if (index != -1) {
                String path = fileUrl.substring(index + pattern.length());
                // Remove query parameters if any
                if (path.contains("?")) {
                    path = path.substring(0, path.indexOf("?"));
                }
                return path;
            }

            // Alternative: if URL already contains just the path after bucket
            if (fileUrl.contains(bucket + "/")) {
                String[] parts = fileUrl.split(bucket + "/");
                if (parts.length > 1) {
                    String path = parts[1];
                    if (path.contains("?")) {
                        path = path.substring(0, path.indexOf("?"));
                    }
                    return path;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting path from URL: {}", fileUrl, e);
            return null;
        }
    }


    /**
     * Delete a file from Supabase Storage (works for both public and private buckets)
     *
     * @param filePathOrUrl The full URL or relative path of the file
     * @param bucket The bucket name
     */
    public void deleteFile(String filePathOrUrl, String bucket) {
        if (filePathOrUrl == null || filePathOrUrl.isBlank() || filePathOrUrl.equals("DELETED")) {
            log.warn("Skipping deletion - file path is null, blank, or already marked as DELETED: {}", filePathOrUrl);
            return;
        }

        // Check if path already contains DELETED_ prefix (modified filename)
        if (filePathOrUrl.contains("DELETED_")) {
            log.warn("Skipping deletion - file path already contains DELETED_ prefix: {}", filePathOrUrl);
            return;
        }

        try {
            // Extract the actual file path from URL if full URL is provided
            String actualFilePath = extractPathFromUrl(filePathOrUrl, bucket);

            // If extractPathFromUrl returns null, try to use the path directly
            if (actualFilePath == null || actualFilePath.isBlank()) {
                // Check if it's already a relative path (doesn't contain http:// or https://)
                if (!filePathOrUrl.startsWith("http://") && !filePathOrUrl.startsWith("https://")) {
                    actualFilePath = filePathOrUrl;
                } else {
                    log.warn("Could not extract file path from: {}", filePathOrUrl);
                    return;
                }
            }

            // Remove leading slash if present (Supabase API doesn't need it)
            if (actualFilePath.startsWith("/")) {
                actualFilePath = actualFilePath.substring(1);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(supabaseKey); // Use service key for private buckets
            // Don't set Content-Type for DELETE requests - Supabase Storage API doesn't require it
            // Setting it to application/json without a body causes 400 Bad Request

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Supabase Storage DELETE endpoint format: /storage/v1/object/{bucket}/{filePath}
            // For private buckets, this works with service key authentication
            // Use UriComponentsBuilder to properly URL-encode the path (handles spaces, special chars, etc.)
            String deleteUrl = UriComponentsBuilder.fromHttpUrl(supabaseUrl)
                    .path("/storage/v1/object/{bucket}/{filePath}")
                    .buildAndExpand(bucket, actualFilePath)
                    .toUriString();

            log.info("Attempting to delete file from Supabase (private bucket): bucket={}, path={}, url={}",
                    bucket, actualFilePath, deleteUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted file from Supabase: bucket={}, path={}", bucket, actualFilePath);
            } else {
                log.warn("Failed to delete file from Supabase. Status: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
                // Don't throw exception - just log the warning (file might already be deleted)
                log.warn("Continuing despite delete failure - file will be marked as DELETED in database");
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle 404 (file not found) or 403 (access denied) gracefully
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("File not found in Supabase (may already be deleted): path={}", filePathOrUrl);
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("Access denied when deleting file from Supabase. Check service key permissions: path={}", filePathOrUrl);
                // Don't throw - mark as DELETED in DB anyway
            } else {
                log.error("HTTP error deleting file from Supabase: status={}, path={}, error={}",
                        e.getStatusCode(), filePathOrUrl, e.getMessage());
            }
            // Don't throw exception - continue to mark as DELETED in database
        } catch (Exception e) {
            log.error("Error deleting file from Supabase: path={}, error={}", filePathOrUrl, e.getMessage(), e);
            // Don't throw exception - continue to mark as DELETED in database
            // The file will be marked as DELETED in the database even if physical deletion fails
        }
    }
}