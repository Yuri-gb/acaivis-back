package com.acaivis.service;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class MercadoPagoService {

    private static final String ORDERS_URL = "https://api.mercadopago.com/v1/orders";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mercadopago.access-token}")
    private String accessToken;

    public MercadoPagoService() {
        this.restTemplate = new RestTemplate();
    }

    public MercadoPagoPaymentResponse criarPagamento(
            BigDecimal valorTotal,
            String referencia,
            MercadoPagoPaymentRequest pagamento
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("MERCADOPAGO_ACCESS_TOKEN não configurado.");
        }

        boolean pix = "pix".equalsIgnoreCase(pagamento.paymentMethodId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Idempotency-Key", pagamento.idempotencyKey() == null || pagamento.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : pagamento.idempotencyKey());

        Map<String, Object> paymentMethod = new HashMap<>();
        paymentMethod.put("id", pix ? "pix" : pagamento.paymentMethodId());
        paymentMethod.put("type", pix ? "bank_transfer" : pagamento.paymentMethodType());

        if (!pix && pagamento.token() != null && !pagamento.token().isBlank()) {
            paymentMethod.put("token", pagamento.token());
        }

        if (!pix && pagamento.installments() != null) {
            paymentMethod.put("installments", pagamento.installments());
        }

        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", valorTotal.toPlainString());
        payment.put("payment_method", paymentMethod);

        if (pix) {
            payment.put("expiration_time", "PT30M");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("type", "online");
        body.put("processing_mode", "automatic");
        body.put("total_amount", valorTotal.toPlainString());
        body.put("external_reference", referencia);
        body.put("payer", Map.of("email", pagamento.payerEmail()));
        body.put("transactions", Map.of("payments", List.of(payment)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    ORDERS_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 402) {
                return parseFailedPayment(exception.getResponseBodyAsString());
            }
            throw exception;
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || responseBody.get("id") == null) {
            throw new IllegalStateException("Mercado Pago não retornou uma order válida.");
        }

        Map<String, Object> paymentResponse = firstPayment(responseBody);
        Map<String, Object> paymentMethodResponse = map(paymentResponse.get("payment_method"));

        String status = string(paymentResponse.get("status"), responseBody.get("status"));
        String statusDetail = string(
                paymentResponse.get("status_detail"),
                responseBody.get("status_detail")
        );

        OffsetDateTime expiresAt = pix
                ? OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30)
                : null;

        return new MercadoPagoPaymentResponse(
                string(responseBody.get("id")),
                string(paymentResponse.get("id")),
                status,
                statusDetail,
                string(paymentMethodResponse.get("qr_code")),
                string(paymentMethodResponse.get("qr_code_base64")),
                string(paymentMethodResponse.get("ticket_url")),
                expiresAt
        );
    }

    private MercadoPagoPaymentResponse parseFailedPayment(String responseBody) {
        try {
            Map<String, Object> root = objectMapper.readValue(
                    responseBody,
                    new TypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> data = map(root.get("data"));
            Map<String, Object> payment = firstPayment(data);
            Map<String, Object> paymentMethod = map(payment.get("payment_method"));

            return new MercadoPagoPaymentResponse(
                    string(data.get("id")),
                    string(payment.get("id")),
                    string(payment.get("status"), data.get("status")),
                    string(payment.get("status_detail"), data.get("status_detail")),
                    string(paymentMethod.get("qr_code")),
                    string(paymentMethod.get("qr_code_base64")),
                    string(paymentMethod.get("ticket_url")),
                    null
            );
        } catch (Exception parseException) {
            return new MercadoPagoPaymentResponse(
                    null,
                    null,
                    "failed",
                    "provider_error",
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public MercadoPagoPaymentResponse toPaymentResponse(Map<String, Object> responseBody) {
        Map<String, Object> paymentResponse = firstPayment(responseBody);
        Map<String, Object> paymentMethodResponse = map(paymentResponse.get("payment_method"));

        String status = string(paymentResponse.get("status"), responseBody.get("status"));
        String statusDetail = string(
                paymentResponse.get("status_detail"),
                responseBody.get("status_detail")
        );

        return new MercadoPagoPaymentResponse(
                string(responseBody.get("id")),
                string(paymentResponse.get("id")),
                status,
                statusDetail,
                string(paymentMethodResponse.get("qr_code")),
                string(paymentMethodResponse.get("qr_code_base64")),
                string(paymentMethodResponse.get("ticket_url")),
                null
        );
    }

    public void reembolsarOrder(String orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        restTemplate.exchange(
                ORDERS_URL + "/" + orderId + "/refund",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );
    }

    public Map<String, Object> buscarOrder(String orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response = restTemplate.exchange(
                ORDERS_URL + "/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private Map<String, Object> firstPayment(Map<String, Object> order) {
        Map<String, Object> transactions = map(order.get("transactions"));
        Object paymentsValue = transactions.get("payments");

        if (!(paymentsValue instanceof List<?> payments) || payments.isEmpty()) {
            return Map.of();
        }

        return map(payments.get(0));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String string(Object first, Object fallback) {
        Object value = first != null ? first : fallback;
        return value == null ? null : String.valueOf(value);
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
