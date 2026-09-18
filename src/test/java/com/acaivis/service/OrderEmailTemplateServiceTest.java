package com.acaivis.service;

import com.acaivis.model.Order;
import com.acaivis.service.OrderEmailEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class OrderEmailTemplateServiceTest {

    private final OrderEmailTemplateService service =
            new OrderEmailTemplateService("https://açaívis.com.br/rastrear-pedido");

    @Test
    void deveGerarTemplateComDadosDoPedido() {
        Order order = order();

        var content = service.build(order, OrderEmailEvent.PAYMENT_CONFIRMED);

        assertTrue(content.subject().contains("AC-TEST01"));
        assertTrue(content.html().contains("Cliente Teste"));
        assertTrue(content.html().contains("2x Açaí"));
        assertTrue(content.html().contains("R$ 25.00"));
        assertTrue(content.html().contains("https://açaívis.com.br/rastrear-pedido?codigo=AC-TEST01"));
    }

    @Test
    void deveEscaparConteudoDoCliente() {
        Order order = order();
        order.setCustomerName("<Cliente>");

        var content = service.build(order, OrderEmailEvent.PAYMENT_CONFIRMED);

        assertTrue(content.html().contains("&lt;Cliente&gt;"));
        assertFalse(content.html().contains("<Cliente>"));
    }

    @Test
    void naoDeveMostrarRastreamentoEmEventosSemRastreamento() {
        Order order = order();

        for (OrderEmailEvent event : new OrderEmailEvent[]{
                OrderEmailEvent.PAYMENT_PENDING,
                OrderEmailEvent.DELIVERED,
                OrderEmailEvent.CANCELLED
        }) {
            var content = service.build(order, event);
            assertFalse(content.html().contains("Acompanhar pedido"), event.name());
            assertFalse(content.html().contains("Código de acompanhamento:"), event.name());
        }
    }

    @Test
    void deveMostrarRastreamentoNosEventosComRastreamento() {
        Order order = order();

        for (OrderEmailEvent event : new OrderEmailEvent[]{
                OrderEmailEvent.PAYMENT_CONFIRMED,
                OrderEmailEvent.PREPARING,
                OrderEmailEvent.READY,
                OrderEmailEvent.OUT_FOR_DELIVERY
        }) {
            var content = service.build(order, event);
            assertTrue(content.html().contains("Acompanhar pedido"), event.name());
            assertTrue(content.html().contains("Código de acompanhamento:"), event.name());
        }
    }

    private Order order() {
        Order order = new Order();
        order.setCustomerName("Cliente Teste");
        order.setCustomerEmail("cliente@example.com");
        order.setTrackingCode("AC-TEST01");
        order.setTotal(new BigDecimal("25.00"));
        order.setItems(new ArrayList<>());

        var item = new com.acaivis.model.OrderItem();
        item.setProductName("Açaí");
        item.setSize("500ml");
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("25.00"));
        order.getItems().add(item);

        return order;
    }
}