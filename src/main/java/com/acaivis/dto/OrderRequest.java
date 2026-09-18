package com.acaivis.dto;
import com.acaivis.model.PaymentMethod; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.List;
public record OrderRequest(@NotBlank String customerName,@NotBlank String customerPhone,@Email String customerEmail,@NotBlank String street,@NotBlank String number,String complement,@NotBlank String neighborhood,@NotBlank String city,@NotBlank @Size(min=2,max=2) String state,@NotBlank String zipCode,Long deliveryZoneId,@NotNull PaymentMethod paymentMethod,String notes,@NotEmpty List<@Valid OrderItemRequest> items) {}
