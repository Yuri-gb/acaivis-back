package com.acaivis.service;

import com.acaivis.model.Order;
import com.acaivis.service.OrderEmailEvent;
import com.acaivis.model.OrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderEmailNotificationServiceTest {

    private JavaMailSender sender;
    private OrderEmailNotificationService service;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        service = new OrderEmailNotificationService(
                sender,
                new OrderEmailTemplateService("https://açaívis.com.br/rastrear-pedido"),
                "no-reply@acaivis.com.br"
        );
    }

    @Test
    void deveEnviarEmailHtmlQuandoPedidoPossuiEmail() {
        Order order = orderWithEmail();
        MimeMessage message = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(message);

        assertTrue(service.send(order, OrderEmailEvent.PAYMENT_CONFIRMED));
        verify(sender).send(message);
    }

    @Test
    void naoDeveEnviarQuandoEmailNaoExiste() {
        Order order = orderWithEmail();
        order.setCustomerEmail(null);

        assertFalse(service.send(order, OrderEmailEvent.PAYMENT_CONFIRMED));
        verify(sender, never()).createMimeMessage();
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void naoDeveEnviarQuandoFromNaoEstaConfigurado() {
        OrderEmailNotificationService noFrom = new OrderEmailNotificationService(
                sender,
                new OrderEmailTemplateService("https://açaívis.com.br/rastrear-pedido"),
                ""
        );

        assertFalse(noFrom.send(orderWithEmail(), OrderEmailEvent.PAYMENT_CONFIRMED));
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void deveFalharSilenciosamenteQuandoServidorDeEmailFalha() {
        Order order = orderWithEmail();
        MimeMessage message = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(message);
        doThrow(new RuntimeException("SMTP indisponível")).when(sender).send(message);

        assertFalse(service.send(order, OrderEmailEvent.PAYMENT_CONFIRMED));
    }

    private Order orderWithEmail() {
        Order order = new Order();
        order.setCustomerName("Cliente Teste");
        order.setCustomerEmail("cliente@example.com");
        order.setTrackingCode("AC-TEST01");
        order.setTotal(new BigDecimal("25.00"));
        // A lista de itens já é inicializada pelo próprio Order.
        return order;
    }
}
