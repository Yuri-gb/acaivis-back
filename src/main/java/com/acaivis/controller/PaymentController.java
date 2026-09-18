package com.acaivis.controller;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.acaivis.exception.BusinessException;
import com.acaivis.exception.ResourceNotFoundException;
import com.acaivis.model.Order;
import com.acaivis.model.OrderStatus;
import com.acaivis.repository.OrderRepository;
import com.acaivis.service.MercadoPagoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final OrderRepository orders;
    private final MercadoPagoService mercadoPago;

    public PaymentController(
            OrderRepository orders,
            MercadoPagoService mercadoPago
    ) {
        this.orders = orders;
        this.mercadoPago = mercadoPago;
    }

    @PostMapping("/orders/{orderId}")
    @Transactional
    public ResponseEntity<MercadoPagoPaymentResponse> criarPagamento(
            @PathVariable Long orderId,
            @Valid @RequestBody MercadoPagoPaymentRequest request
    ) {

        Order order = orders.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido não encontrado: " + orderId
                        )
                );

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException(
                    "Este pedido já está pago."
            );
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(
                    "Não é possível pagar um pedido cancelado."
            );
        }

        if (order.getCustomerEmail() == null
                || order.getCustomerEmail().isBlank()) {
            throw new BusinessException(
                    "O pedido precisa ter um e-mail do cliente."
            );
        }

        MercadoPagoPaymentRequest pagamento =
                new MercadoPagoPaymentRequest(
                        request.paymentMethodId(),
                        request.paymentMethodType(),
                        request.token(),
                        request.installments(),
                        order.getCustomerEmail()
                );

        MercadoPagoPaymentResponse response =
                mercadoPago.criarPagamento(
                        order.getTotal(),
                        order.getTrackingCode(),
                        pagamento
                );

        order.setMercadoPagoOrderId(
                response.orderId()
        );

        orders.save(order);

        return ResponseEntity.ok(response);
    }
}