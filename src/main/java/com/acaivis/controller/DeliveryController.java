package com.acaivis.controller;
import com.acaivis.dto.*; import com.acaivis.service.DeliveryService; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.security.Principal; import java.util.List;
@RestController @RequestMapping("/api/delivery") public class DeliveryController {
 private final DeliveryService service; public DeliveryController(DeliveryService s){service=s;}
 @GetMapping("/calculate") public DeliveryCalculationResponse calculate(@RequestParam String zipCode){return service.calculate(zipCode);}
 @GetMapping("/orders") @PreAuthorize("hasRole('DELIVERER')") public List<DeliveryOrderResponse> orders(){return service.orders();}
 @PatchMapping("/orders/{id}/route") @PreAuthorize("hasRole('DELIVERER')") public DeliveryOrderResponse route(@PathVariable Long id,@RequestParam Integer order){return service.setRouteOrder(id,order);}
 @PostMapping(value="/orders/{id}/deliver",consumes="multipart/form-data") @PreAuthorize("hasRole('DELIVERER')") public DeliveryOrderResponse deliver(@PathVariable Long id,@RequestPart("proof") MultipartFile proof,Principal principal){return service.deliver(id,proof,principal);}
}
