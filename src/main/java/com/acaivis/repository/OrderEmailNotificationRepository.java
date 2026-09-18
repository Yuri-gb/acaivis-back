package com.acaivis.repository;

import com.acaivis.model.OrderEmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderEmailNotificationRepository
        extends JpaRepository<OrderEmailNotification, Long> {

    Optional<OrderEmailNotification> findByOrderIdAndEvent(Long orderId, String event);
}
