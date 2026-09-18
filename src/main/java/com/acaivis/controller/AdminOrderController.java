package com.acaivis.controller;
import com.acaivis.dto.AdminOrderPageResponse; import com.acaivis.dto.OrderResponse; import com.acaivis.model.OrderStatus; import com.acaivis.service.OrderService; import org.springframework.data.domain.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/orders") @PreAuthorize("hasRole('ADMIN')") public class AdminOrderController {
 private final OrderService service; public AdminOrderController(OrderService s){service=s;}
 @GetMapping public AdminOrderPageResponse page(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="") String search,@RequestParam(required=false) OrderStatus status){return service.adminPage(search,status,PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),50)));}
 @PatchMapping("/{id}/mark-paid") public OrderResponse markPaid(@PathVariable Long id){return service.markPaymentOnDeliveryAsPaid(id);}
}
