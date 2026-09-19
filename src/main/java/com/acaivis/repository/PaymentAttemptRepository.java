package com.acaivis.repository;

import com.acaivis.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, String> {
    Optional<PaymentAttempt> findByMercadoPagoOrderId(String mercadoPagoOrderId);
}
