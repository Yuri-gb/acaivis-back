package com.acaivis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String secretKey;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.secret-key:}") String secretKey) {
        this.supabaseUrl = supabaseUrl;
        this.secretKey = secretKey;
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .build();
    }

    public UploadedImage uploadProductImage(MultipartFile file) {
        validateConfiguration();
        validateFile(file);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extensionFor(contentType);
        String path = "products/" + UUID.randomUUID() + extension;

        try {
            restClient.post()
                    .uri("/storage/v1/object/product-images/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header("apikey", secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header("x-upsert", "false")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler a imagem enviada.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível enviar a imagem para o Storage.", e);
        }

        String publicUrl = supabaseUrl
                + "/storage/v1/object/public/product-images/"
                + path;

        return new UploadedImage(publicUrl, path);
    }

    private void validateConfiguration() {
        if (supabaseUrl.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException("Supabase Storage não está configurado.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A imagem é obrigatória.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("A imagem deve ter no máximo 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowed(contentType)) {
            throw new IllegalArgumentException("Formato de imagem não permitido. Use JPEG, PNG ou WebP.");
        }
    }

    private boolean isAllowed(String contentType) {
        return MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)
                || MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)
                || "image/webp".equalsIgnoreCase(contentType);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Formato de imagem não permitido.");
        };
    }


    public UploadedImage uploadDeliveryProof(MultipartFile file) {
        validateConfiguration();
        validateFile(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extensionFor(contentType);
        String path = "delivery-proofs/" + UUID.randomUUID() + extension;
        try {
            restClient.post()
                    .uri("/storage/v1/object/delivery-proofs/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header("apikey", secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header("x-upsert", "false")
                    .body(file.getBytes())
                    .retrieve().toBodilessEntity();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler a foto da entrega.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível enviar a foto da entrega para o Storage.", e);
        }
        String publicUrl = supabaseUrl + "/storage/v1/object/public/delivery-proofs/" + path;
        return new UploadedImage(publicUrl, path);
    }

    public record UploadedImage(String imageUrl, String path) {
    }
}
