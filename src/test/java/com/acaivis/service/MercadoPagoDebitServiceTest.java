package com.acaivis.service;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MercadoPagoDebitServiceTest {

    @Test
    void deveEnviarDebitoSemParcelasEComToken() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MercadoPagoDebitService service = new MercadoPagoDebitService(restTemplate);
        field(service, "accessToken", "token");

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of(
                "id", "order-1",
                "status", "processed",
                "transactions", Map.of("payments", List.of(Map.of(
                        "id", "payment-1",
                        "status", "processed",
                        "status_detail", "accredited",
                        "payment_method", Map.of("id", "elo", "type", "debit_card")
                )))
        )));

        var request = new MercadoPagoPaymentRequest(
                "elo", "debit_card", "token-1", null,
                "cliente@teste.com", "idem-1"
        );

        var response = service.criarPagamento(
                new BigDecimal("25.00"), "card-debit-test", request
        );

        assertEquals("order-1", response.orderId());
        assertEquals("processed", response.status());

        var captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(Map.class)
        );

        Map<?, ?> body = (Map<?, ?>) captor.getValue().getBody();
        Map<?, ?> transactions = (Map<?, ?>) body.get("transactions");
        Map<?, ?> payment = (Map<?, ?>) ((List<?>) transactions.get("payments")).get(0);
        Map<?, ?> method = (Map<?, ?>) payment.get("payment_method");

        assertEquals("debelo", method.get("id"));
        assertEquals("debit_card", method.get("type"));
        assertEquals("token-1", method.get("token"));
        assertEquals(1, method.get("installments"));
    }

    @Test
    void deveRejeitarTokenAusente() throws Exception {
        MercadoPagoDebitService service =
                new MercadoPagoDebitService(mock(RestTemplate.class));
        field(service, "accessToken", "token");

        var request = new MercadoPagoPaymentRequest(
                "elo", "debit_card", null, null,
                "cliente@teste.com", "idem"
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.criarPagamento(
                        new BigDecimal("25.00"), "card-debit-test", request
                )
        );
    }

    @Test
    void deveRejeitarTipoDiferenteDeDebito() throws Exception {
        MercadoPagoDebitService service =
                new MercadoPagoDebitService(mock(RestTemplate.class));
        field(service, "accessToken", "token");

        var request = new MercadoPagoPaymentRequest(
                "elo", "credit_card", "token", 1,
                "cliente@teste.com", "idem"
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.criarPagamento(
                        new BigDecimal("25.00"), "card-debit-test", request
                )
        );
    }

    private static void field(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
