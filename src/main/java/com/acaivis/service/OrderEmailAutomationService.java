package com.acaivis.service;

import com.acaivis.model.Order;
import com.acaivis.model.OrderEmailEvent;
import com.acaivis.model.OrderStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderEmailAutomationService {

    private final OrderEmailNotificationService notifications;

    public OrderEmailAutomationService(OrderEmailNotificationService notifications) {
        this.notifications = notifications;
    }

    public void notifyPaymentPending(Order order) {
        notifications.send(order, OrderEmailEvent.PAYMENT_PENDING);
    }

    public void notifyPaymentConfirmed(Order order) {
        notifications.send(order, OrderEmailEvent.PAYMENT_CONFIRMED);
    }

    public void notifyStatus(Order order) {
        OrderStatus status = order.getStatus();
        switch (status) {
            case PREPARING -> notifications.send(order, OrderEmailEvent.PREPARING);
            case READY -> notifications.send(order, OrderEmailEvent.READY);
            case OUT_FOR_DELIVERY -> notifications.send(order, OrderEmailEvent.OUT_FOR_DELIVERY);
            case DELIVERED -> notifications.send(order, OrderEmailEvent.DELIVERED);
            case CANCELLED -> notifications.send(order, OrderEmailEvent.CANCELLED);
            default -> { }
        }
    }
}
