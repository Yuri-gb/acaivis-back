package com.acaivis.controller;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.acaivis.exception.BusinessException;
import com.acaivis.model.Order;
import com.acaivis.model.OrderStatus;
import com.acaivis.repository.OrderRepository;
import com.acaivis.service.MercadoPagoService;
import com.acaivis.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    OrderRepository orders = mock(OrderRepository.class);
    MercadoPagoService mercadoPago = mock(MercadoPagoService.class);
    OrderService orderService = mock(OrderService.class);

    PaymentController controller = new PaymentController(orders, mercadoPago, orderService);

    @Test
    void deveCriarPagamentoEEncaminharEmailDoPedido() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        order.setCustomerEmail("cliente@teste.com");
        when(orders.findById(10L)).thenReturn(Optional.of(order));

        MercadoPagoPaymentRequest request =
                new MercadoPagoPaymentRequest("pix", "bank_transfer", null, null, "outro@email.com", "idem-10");

        MercadoPagoPaymentResponse response = new MercadoPagoPaymentResponse(
                "mp-order-10", "mp-payment-10", "pending", "waiting",
                "pix-code", "base64", "ticket-url", OffsetDateTime.now().plusMinutes(30)
        );

        when(mercadoPago.criarPagamento(order.getTotal(), order.getTrackingCode(),
                new MercadoPagoPaymentRequest("pix", "bank_transfer", null, null, "cliente@teste.com", "idem-10")))
                .thenReturn(response);

        ResponseEntity<MercadoPagoPaymentResponse> result =
                controller.criarPagamento(10L, request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
        assertEquals("mp-order-10", order.getMercadoPagoOrderId());
        assertEquals("mp-payment-10", order.getMercadoPagoPaymentId());
        verify(orders).save(order);
        verify(orderService, never()).cancelarPagamentoFalho(10L);
    }

    @Test
    void pedidoPagoNaoCriaNovoPagamento() {
        Order order = order(OrderStatus.PAID);
        when(orders.findById(11L)).thenReturn(Optional.of(order));

        MercadoPagoPaymentRequest request =
                new MercadoPagoPaymentRequest("pix", "bank_transfer", null, null, "x@y.com", "idem");

        assertThrows(BusinessException.class, () -> controller.criarPagamento(11L, request));
        verifyNoInteractions(mercadoPago);
    }

    @Test
    void falhaAoCriarPagamentoCancelaPedido() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        when(orders.findById(12L)).thenReturn(Optional.of(order));
        when(mercadoPago.criarPagamento(any(), any(), any()))
                .thenThrow(new IllegalStateException("MP indisponível"));

        MercadoPagoPaymentRequest request =
                new MercadoPagoPaymentRequest("pix", "bank_transfer", null, null, "x@y.com", "idem");

        assertThrows(IllegalStateException.class, () -> controller.criarPagamento(12L, request));
        verify(orderService).cancelarPagamentoFalho(12L);
        verify(orders, never()).save(order);
    }

    @Test
    void webhookComAssinaturaValidaProcessaPedido() throws Exception {
        String secret = "segredo-teste";
        setField(controller, "webhookSecret", secret);

        String dataId = "123456";
        String requestId = "request-abc";
        String ts = "1720000000";
        String manifest = "id:" + dataId.toLowerCase()
                + ";request-id:" + requestId
                + ";ts:" + ts + ";";
        String signature = hmacHex(secret, manifest);

        ResponseEntity<Void> result = controller.webhook(
                dataId,
                "ts=" + ts + ",v1=" + signature,
                requestId
        );

        assertEquals(200, result.getStatusCode().value());
        verify(orderService).processarWebhookMercadoPago(dataId);
    }

    @Test
    void webhookComAssinaturaInvalidaNaoProcessaPedido() throws Exception {
        setField(controller, "webhookSecret", "segredo-teste");

        ResponseEntity<Void> result = controller.webhook(
                "123456",
                "ts=1720000000,v1=assinatura-errada",
                "request-abc"
        );

        assertEquals(401, result.getStatusCode().value());
        verifyNoInteractions(orderService);
    }

    @Test
    void statusDePedidoNormalizaCodigoDeRastreamento() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        order.setMercadoPagoOrderId("mp-order");
        order.setMercadoPagoPaymentId("mp-payment");
        order.setPaymentStatus("pending");
        order.setPaymentStatusDetail("waiting");
        when(orders.findByTrackingCode("AC123456")).thenReturn(Optional.of(order));

        ResponseEntity<MercadoPagoPaymentResponse> result =
                controller.status(" ac123456 ");

        assertEquals(200, result.getStatusCode().value());
        assertEquals("mp-order", result.getBody().orderId());
        assertEquals("mp-payment", result.getBody().paymentId());
        assertEquals("pending", result.getBody().status());
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setStatus(status);
        order.setTrackingCode("AC123456");
        order.setCustomerEmail("x@y.com");
        return order;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String hmacHex(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
