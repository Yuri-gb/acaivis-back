package com.acaivis.controller;

import com.acaivis.dto.ProductPageResponse;
import com.acaivis.dto.ProductResponse;
import com.acaivis.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ProductPageResponse page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean available
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return service.findAdminPage(search, categoryId, available, pageable);
    }

    @PatchMapping("/{id}/reactivate")
    public ProductResponse reactivate(@PathVariable Long id) {
        return service.reactivate(id);
    }
}
