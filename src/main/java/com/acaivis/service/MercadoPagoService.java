package com.acaivis.service;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class MercadoPagoService {

    private static final String ORDERS_URL =
            "https://api.mercadopago.com/v1/orders";

    private final RestTemplate restTemplate;

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

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set(
                "X-Idempotency-Key",
                UUID.randomUUID().toString()
        );

        Map<String, Object> paymentMethod = new java.util.HashMap<>();

        paymentMethod.put(
                "id",
                pagamento.paymentMethodId()
        );

        paymentMethod.put(
                "type",
                pagamento.paymentMethodType()
        );

        if (pagamento.token() != null &&
                !pagamento.token().isBlank()) {

            paymentMethod.put(
                    "token",
                    pagamento.token()
            );
        }

        if (pagamento.installments() != null) {

            paymentMethod.put(
                    "installments",
                    pagamento.installments()
            );
        }

        Map<String, Object> payment = Map.of(
                "amount", valorTotal,
                "payment_method", paymentMethod
        );

        Map<String, Object> transactions = Map.of(
                "payments",
                java.util.List.of(payment)
        );

        Map<String, Object> payer = Map.of(
                "email",
                pagamento.payerEmail()
        );

        Map<String, Object> body = Map.of(
                "type", "online",
                "processing_mode", "automatic",
                "total_amount", valorTotal,
                "external_reference", referencia,
                "payer", payer,
                "transactions", transactions
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        var response = restTemplate.exchange(
                ORDERS_URL,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> responseBody =
                response.getBody();

        if (responseBody == null) {
            throw new IllegalStateException(
                    "Mercado Pago não retornou uma resposta."
            );
        }

        String orderId =
                String.valueOf(responseBody.get("id"));

        String status =
                String.valueOf(responseBody.get("status"));

        String statusDetail =
                responseBody.get("status_detail") != null
                        ? String.valueOf(
                                responseBody.get("status_detail")
                        )
                        : null;

        return new MercadoPagoPaymentResponse(
                orderId,
                status,
                statusDetail,
                null,
                null
        );
    }
}