package com.acaivis.service;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class MercadoPagoDebitService {
    private static final String ORDERS_URL = "https://api.mercadopago.com/v1/orders";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mercadopago.access-token}")
    private String accessToken;

    public MercadoPagoDebitService() { this(new RestTemplate()); }
    MercadoPagoDebitService(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public MercadoPagoPaymentResponse criarPagamento(BigDecimal valorTotal, String referencia,
                                                     MercadoPagoPaymentRequest pagamento) {
        if (accessToken == null || accessToken.isBlank())
            throw new IllegalStateException("MERCADOPAGO_ACCESS_TOKEN não configurado.");
        if (!"debit_card".equalsIgnoreCase(pagamento.paymentMethodType()))
            throw new IllegalArgumentException("Este serviço aceita somente cartão de débito.");
        if (pagamento.token() == null || pagamento.token().isBlank())
            throw new IllegalArgumentException("Token do cartão de débito é obrigatório.");
        if (pagamento.paymentMethodId() == null || pagamento.paymentMethodId().isBlank())
            throw new IllegalArgumentException("Identificador do cartão de débito é obrigatório.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Idempotency-Key",
                pagamento.idempotencyKey() == null || pagamento.idempotencyKey().isBlank()
                        ? UUID.randomUUID().toString() : pagamento.idempotencyKey());

        Map<String,Object> paymentMethod = new HashMap<>();
        paymentMethod.put("id", pagamento.paymentMethodId());
        paymentMethod.put("type", "debit_card");
        paymentMethod.put("token", pagamento.token());
        paymentMethod.put("installments", 1);

        Map<String,Object> payment = new HashMap<>();
        payment.put("amount", valorTotal.toPlainString());
        payment.put("payment_method", paymentMethod);

        Map<String,Object> body = new HashMap<>();
        body.put("type", "online");
        body.put("processing_mode", "automatic");
        body.put("total_amount", valorTotal.toPlainString());
        body.put("external_reference", referencia);

        Map<String,Object> payer = new HashMap<>();
        payer.put("email", pagamento.payerEmail());
        if (pagamento.payerIdentificationType() != null
                && !pagamento.payerIdentificationType().isBlank()
                && pagamento.payerIdentificationNumber() != null
                && !pagamento.payerIdentificationNumber().isBlank()) {
            payer.put("identification", Map.of(
                    "type", pagamento.payerIdentificationType(),
                    "number", pagamento.payerIdentificationNumber()
            ));
        }

        body.put("payer", payer);
        body.put("transactions", Map.of("payments", List.of(payment)));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    ORDERS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<String,Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("id") == null)
                throw new IllegalStateException("Mercado Pago não retornou uma order válida.");
            return toPaymentResponse(responseBody);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 402)
                return parseFailedPayment(exception.getResponseBodyAsString());
            throw exception;
        }
    }

    private MercadoPagoPaymentResponse parseFailedPayment(String responseBody) {
        try {
            Map<String,Object> root = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String,Object>>() {});
            return toPaymentResponse(map(root.get("data")));
        } catch (Exception ignored) {
            return new MercadoPagoPaymentResponse(null,null,"failed","provider_error",
                    null,null,null,null);
        }
    }

    private MercadoPagoPaymentResponse toPaymentResponse(Map<String,Object> responseBody) {
        Map<String,Object> payment = firstPayment(responseBody);
        Map<String,Object> method = map(payment.get("payment_method"));
        return new MercadoPagoPaymentResponse(
                string(responseBody.get("id")),
                string(payment.get("id")),
                string(payment.get("status"), responseBody.get("status")),
                string(payment.get("status_detail"), responseBody.get("status_detail")),
                string(method.get("qr_code")), string(method.get("qr_code_base64")),
                string(method.get("ticket_url")), null);
    }

    private Map<String,Object> firstPayment(Map<String,Object> order) {
        Map<String,Object> transactions = map(order.get("transactions"));
        Object payments = transactions.get("payments");
        if (!(payments instanceof List<?> list) || list.isEmpty()) return Map.of();
        return map(list.get(0));
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> map(Object value) {
        if (value instanceof Map<?,?> map) return (Map<String,Object>) map;
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
