package com.acaivis.service;

import com.acaivis.model.Order;
import com.acaivis.service.OrderEmailEvent;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.acaivis.repository.OrderEmailNotificationRepository;

@Service
public class OrderEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailNotificationService.class);

    private final JavaMailSender sender;
    private final OrderEmailTemplateService templates;
    private final String from;
    private final OrderEmailNotificationRepository notifications;

    public OrderEmailNotificationService(
            JavaMailSender sender,
            OrderEmailTemplateService templates,
            OrderEmailNotificationRepository notifications,
            @Value("${mail.from:}") String from) {
        this.sender = sender;
        this.templates = templates;
        this.from = from;
        this.notifications = notifications;
    }

    public boolean send(Order order, OrderEmailEvent event) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            log.warn("E-mail não enviado: pedido {} não possui e-mail do cliente", order.getId());
            return false;
        }

        if (from.isBlank()) {
            log.warn("E-mail não enviado: MAIL_FROM não configurado para o pedido {}", order.getId());
            return false;
        }

        var eventName = event.name();
        var existing = notifications.findByOrderIdAndEvent(order.getId(), eventName);
        if (existing.isPresent() && "SENT".equals(existing.get().getStatus())) {
            log.info("E-mail já enviado: pedido {} / evento {}", order.getId(), eventName);
            return true;
        }

        var notification = existing.orElseGet(() ->
                new com.acaivis.model.OrderEmailNotification(order, eventName));

        try {
            notification = notifications.saveAndFlush(notification);
            var content = templates.build(order, event);
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(from);
            helper.setTo(order.getCustomerEmail());
            helper.setSubject(content.subject());
            helper.setText(content.html(), true);

            sender.send(message);
            notification.markSent();
            notifications.save(notification);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("E-mail já registrado por outra execução: pedido {} / evento {}", order.getId(), eventName);
            return notifications.findByOrderIdAndEvent(order.getId(), eventName)
                    .map(n -> "SENT".equals(n.getStatus()))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail: pedido {} / evento {}", order.getId(), eventName, e);
            return false;
        }
    }
}
