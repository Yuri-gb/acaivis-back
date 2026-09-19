package com.acaivis.service;

import com.acaivis.dto.payment.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MercadoPagoServiceTest {

    @Test
    void deveCriarPixComExpiracaoEIdempotencia() throws Exception {
        RestTemplate rt = mock(RestTemplate.class);
        MercadoPagoService s = new MercadoPagoService();
        field(s, "restTemplate", rt);
        field(s, "accessToken", "token");

        Map<String, Object> payment = Map.of(
                "id", "pay-1",
                "status", "pending",
                "status_detail", "waiting",
                "payment_method", Map.of(
                        "qr_code", "code",
                        "qr_code_base64", "b64",
                        "ticket_url", "url"
                )
        );

        when(rt.exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of(
                "id", "ord-1",
                "status", "pending",
                "transactions", Map.of("payments", List.of(payment))
        )));

        var req = new MercadoPagoPaymentRequest(
                "pix", "bank_transfer", null, null, "x@y.com", "idem-1"
        );

        var res = s.criarPagamento(new BigDecimal("25.90"), "AC123456", req);

        assertEquals("ord-1", res.orderId());
        assertEquals("pay-1", res.paymentId());
        assertEquals("code", res.qrCode());
        assertNotNull(res.expiresAt());

        ArgumentCaptor<HttpEntity> c = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rt).exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                c.capture(),
                eq(Map.class)
        );

        assertEquals("Bearer token", c.getValue().getHeaders().getFirst("Authorization"));
        assertEquals("idem-1", c.getValue().getHeaders().getFirst("X-Idempotency-Key"));

        Map<?, ?> body = (Map<?, ?>) c.getValue().getBody();
        Map<?, ?> tx = (Map<?, ?>) body.get("transactions");
        Map<?, ?> sent = (Map<?, ?>) ((List<?>) tx.get("payments")).get(0);

        assertEquals("25.90", sent.get("amount"));
        assertEquals("25.90", body.get("total_amount"));
        assertEquals("PT30M", sent.get("expiration_time"));
    }

    @Test
    void deveTransformarRecusaDoMercadoPagoEmResultadoFalho() throws Exception {
        RestTemplate rt = mock(RestTemplate.class);
        MercadoPagoService s = new MercadoPagoService();
        field(s, "restTemplate", rt);
        field(s, "accessToken", "token");

        String body = """
                {
                  "errors": [{"code":"failed","message":"The following transactions failed","details":["pay-1: invalid_card_token"]}],
                  "data": {
                    "id": "ord-failed",
                    "status": "failed",
                    "status_detail": "failed",
                    "transactions": {
                      "payments": [{
                        "id": "pay-1",
                        "status": "failed",
                        "status_detail": "invalid_card_token",
                        "payment_method": {"id":"master","type":"credit_card"}
                      }]
                    }
                  }
                }
                """;

        when(rt.exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.PAYMENT_REQUIRED,
                "Payment Required",
                HttpHeaders.EMPTY,
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8
        ));

        var req = new MercadoPagoPaymentRequest(
                "master", "credit_card", "token-1", 1, "x@y.com", "idem-1"
        );

        var res = s.criarPagamento(new BigDecimal("25.00"), "card-checkout-test", req);

        assertEquals("ord-failed", res.orderId());
        assertEquals("pay-1", res.paymentId());
        assertEquals("failed", res.status());
        assertEquals("invalid_card_token", res.statusDetail());
    }

    @Test
    void deveFalharSemAccessToken() throws Exception {
        MercadoPagoService s = new MercadoPagoService();
        field(s, "accessToken", "");

        var req = new MercadoPagoPaymentRequest(
                "pix", "bank_transfer", null, null, "x@y.com", "idem"
        );

        assertThrows(
                IllegalStateException.class,
                () -> s.criarPagamento(new BigDecimal("10"), "AC123456", req)
        );
    }

    @Test
    void deveMapearEloParaDebeloQuandoForCartaoDeDebito() throws Exception {
        RestTemplate rt = mock(RestTemplate.class);
        MercadoPagoService s = new MercadoPagoService();
        field(s, "restTemplate", rt);
        field(s, "accessToken", "token");

        Map<String, Object> payment = Map.of(
                "id", "pay-debelo-1",
                "status", "processed",
                "status_detail", "accredited",
                "payment_method", Map.of(
                        "id", "debelo",
                        "type", "debit_card"
                )
        );

        when(rt.exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of(
                "id", "ord-debelo-1",
                "status", "processed",
                "transactions", Map.of("payments", List.of(payment))
        )));

        var req = new MercadoPagoPaymentRequest(
                "elo", "debit_card", "token-debelo-1", null, "x@y.com", "idem-debelo-1"
        );

        s.criarPagamento(new BigDecimal("25.00"), "card-debelo-test", req);

        ArgumentCaptor<HttpEntity> c = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rt).exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                c.capture(),
                eq(Map.class)
        );

        Map<?, ?> body = (Map<?, ?>) c.getValue().getBody();
        Map<?, ?> tx = (Map<?, ?>) body.get("transactions");
        Map<?, ?> sent = (Map<?, ?>) ((List<?>) tx.get("payments")).get(0);
        Map<?, ?> paymentMethod = (Map<?, ?>) sent.get("payment_method");

        assertEquals("debelo", paymentMethod.get("id"));
        assertEquals("debit_card", paymentMethod.get("type"));
        assertEquals("token-debelo-1", paymentMethod.get("token"));
        assertFalse(paymentMethod.containsKey("installments"));
    }

    @Test
    void naoDeveEnviarParcelasParaCartaoDeDebito() throws Exception {
        RestTemplate rt = mock(RestTemplate.class);
        MercadoPagoService s = new MercadoPagoService();
        field(s, "restTemplate", rt);
        field(s, "accessToken", "token");

        Map<String, Object> payment = Map.of(
                "id", "pay-debit-1",
                "status", "processed",
                "status_detail", "accredited",
                "payment_method", Map.of(
                        "id", "visa",
                        "type", "debit_card"
                )
        );

        when(rt.exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of(
                "id", "ord-debit-1",
                "status", "processed",
                "transactions", Map.of("payments", List.of(payment))
        )));

        var req = new MercadoPagoPaymentRequest(
                "visa", "debit_card", "token-debit-1", 1, "x@y.com", "idem-debit-1"
        );

        s.criarPagamento(new BigDecimal("25.00"), "card-debit-test", req);

        ArgumentCaptor<HttpEntity> c = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rt).exchange(
                eq("https://api.mercadopago.com/v1/orders"),
                eq(HttpMethod.POST),
                c.capture(),
                eq(Map.class)
        );

        Map<?, ?> body = (Map<?, ?>) c.getValue().getBody();
        Map<?, ?> tx = (Map<?, ?>) body.get("transactions");
        Map<?, ?> sent = (Map<?, ?>) ((List<?>) tx.get("payments")).get(0);
        Map<?, ?> paymentMethod = (Map<?, ?>) sent.get("payment_method");

        assertEquals("visa", paymentMethod.get("id"));
        assertEquals("debit_card", paymentMethod.get("type"));
        assertEquals("token-debit-1", paymentMethod.get("token"));
        assertFalse(paymentMethod.containsKey("installments"));
    }

    static void field(Object o, String n, Object v) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        f.set(o, v);
    }
}
