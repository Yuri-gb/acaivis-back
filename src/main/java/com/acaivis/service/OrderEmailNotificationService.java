package com.acaivis.service;

import com.acaivis.model.Order;
import com.acaivis.model.OrderEmailEvent;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderEmailNotificationService {

    private final JavaMailSender sender;
    private final OrderEmailTemplateService templates;
    private final String from;

    public OrderEmailNotificationService(
            JavaMailSender sender,
            OrderEmailTemplateService templates,
            @Value("${mail.from:}") String from) {
        this.sender = sender;
        this.templates = templates;
        this.from = from;
    }

    public boolean send(Order order, OrderEmailEvent event) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank() || from.isBlank()) {
            return false;
        }

        try {
            var content = templates.build(order, event);
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(from);
            helper.setTo(order.getCustomerEmail());
            helper.setSubject(content.subject());
            helper.setText(content.html(), true);

            sender.send(message);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
