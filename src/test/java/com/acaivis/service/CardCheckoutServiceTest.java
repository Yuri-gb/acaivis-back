package com.acaivis.service;

import com.acaivis.dto.OrderItemRequest;
import com.acaivis.dto.OrderRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.acaivis.model.DeliveryZone;
import com.acaivis.model.PaymentMethod;
import com.acaivis.model.Product;
import com.acaivis.repository.DeliveryZoneRepository;
import com.acaivis.repository.OrderRepository;
import com.acaivis.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardCheckoutServiceTest {

    DeliveryZoneRepository zones = mock(DeliveryZoneRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    MercadoPagoService mercadoPago = mock(MercadoPagoService.class);
    MercadoPagoDebitService mercadoPagoDebit = mock(MercadoPagoDebitService.class);
    OrderService orderService = mock(OrderService.class);
    PaymentAttemptService paymentAttempts = mock(PaymentAttemptService.class);

    @Test
    void pagamentoEmProcessamentoNaoCriaPedidoLocal() {
        CardCheckoutService service = new CardCheckoutService(
                zones,
                products,
                orders,
                mercadoPago,
                mercadoPagoDebit,
                orderService,
                paymentAttempts
        );

        DeliveryZone zone = new DeliveryZone();
        zone.setActive(true);
        zone.setFee(new BigDecimal("5.00"));
        when(zones.findById(1L)).thenReturn(Optional.of(zone));

        Product product = new Product();
        product.setAvailable(true);
        product.setStockQuantity(10);
        product.setPrice(new BigDecimal("20.00"));
        product.setName("Açaí");
        product.setSize("300ml");
        when(products.findById(1L)).thenReturn(Optional.of(product));

        MercadoPagoPaymentResponse processing = new MercadoPagoPaymentResponse(
                "mp-order-1", "mp-payment-1", "processing", "in_process",
                null, null, null, null
        );
        when(mercadoPago.criarPagamento(
                any(BigDecimal.class), anyString(), any(MercadoPagoPaymentRequest.class)
        )).thenReturn(processing);

        OrderRequest request = new OrderRequest(
                "Cliente", "71999999999", "cliente@email.com", "Rua A", "10", null,
                "Centro", "Feira de Santana", "BA", "44000000", 1L,
                PaymentMethod.CREDIT_CARD, null,
                List.of(new OrderItemRequest(1L, 1))
        );

        var response = service.checkout(
                request,
                new MercadoPagoPaymentRequest(
                        "master", "credit_card", "token", 1,
                        "cliente@email.com", "idem-1"
                )
        );

        assertNull(response.order());
        verify(mercadoPago).criarPagamento(
                any(BigDecimal.class), anyString(), any(MercadoPagoPaymentRequest.class)
        );
        verifyNoInteractions(mercadoPagoDebit);
        verify(paymentAttempts).register(request, new BigDecimal("25.00"), processing);
        verify(orderService, never()).create(any());
    }

    @Test
    void cartaoDeDebitoUsaSomenteOFluxoNovo() {
        CardCheckoutService service = new CardCheckoutService(
                zones,
                products,
                orders,
                mercadoPago,
                mercadoPagoDebit,
                orderService,
                paymentAttempts
        );

        DeliveryZone zone = new DeliveryZone();
        zone.setActive(true);
        zone.setFee(new BigDecimal("5.00"));
        when(zones.findById(1L)).thenReturn(Optional.of(zone));

        Product product = new Product();
        product.setAvailable(true);
        product.setStockQuantity(10);
        product.setPrice(new BigDecimal("20.00"));
        product.setName("Açaí");
        product.setSize("300ml");
        when(products.findById(1L)).thenReturn(Optional.of(product));

        MercadoPagoPaymentResponse processing = new MercadoPagoPaymentResponse(
                "mp-debit-order", "mp-debit-payment", "processing", "in_process",
                null, null, null, null
        );
        when(mercadoPagoDebit.criarPagamento(
                any(BigDecimal.class), anyString(), any(MercadoPagoPaymentRequest.class)
        )).thenReturn(processing);

        OrderRequest request = new OrderRequest(
                "Cliente", "71999999999", "cliente@email.com", "Rua A", "10", null,
                "Centro", "Feira de Santana", "BA", "44000000", 1L,
                PaymentMethod.DEBIT_CARD, null,
                List.of(new OrderItemRequest(1L, 1))
        );

        var response = service.checkout(
                request,
                new MercadoPagoPaymentRequest(
                        "elo", "debit_card", "token", null,
                        "cliente@email.com", "idem-debit-1"
                )
        );

        assertNull(response.order());
        verify(mercadoPagoDebit).criarPagamento(
                any(BigDecimal.class), anyString(), any(MercadoPagoPaymentRequest.class)
        );
        verifyNoInteractions(mercadoPago);
        verify(paymentAttempts).register(request, new BigDecimal("25.00"), processing);
        verify(orderService, never()).create(any());
    }
}