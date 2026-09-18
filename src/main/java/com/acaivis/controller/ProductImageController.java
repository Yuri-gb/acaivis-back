package com.acaivis.controller;

import com.acaivis.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
public class ProductImageController {

    private final SupabaseStorageService storageService;

    public ProductImageController(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(
            summary = "Enviar imagem de produto",
            description = "Envia uma imagem JPEG, PNG ou WebP para o Supabase Storage e retorna a URL pública.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Imagem enviada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Arquivo inválido")
            }
    )
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public SupabaseStorageService.UploadedImage upload(@RequestPart("file") MultipartFile file) {
        return storageService.uploadProductImage(file);
    }
}
