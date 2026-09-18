package com.acaivis.service;

import com.acaivis.dto.*;
import com.acaivis.exception.*;
import com.acaivis.model.*;
import com.acaivis.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private static final char[] TRACK_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    private final OrderRepository orders;
    private final ProductRepository products;
    private final DeliveryZoneRepository zones;
    private final OrderStatusHistoryRepository history;
    private final EmailService email;
    private final MercadoPagoService mercadoPago;
    private final DeliveryService delivery;

    public OrderService(OrderRepository o, ProductRepository p, DeliveryZoneRepository z, OrderStatusHistoryRepository h, EmailService e, MercadoPagoService mercadoPago, DeliveryService delivery) {
        orders = o; products = p; zones = z; history = h; email = e; this.mercadoPago = mercadoPago; this.delivery = delivery;
    }

    @Transactional
    public OrderResponse create(OrderRequest d) {
        var deliveryCalculation = delivery.calculate(d.zipCode());
        DeliveryZone zone = zones.findById(deliveryCalculation.deliveryZoneId()).orElseThrow(() -> new ResourceNotFoundException("Região de entrega não encontrada"));
        if (!zone.isActive()) throw new BusinessException("Região de entrega indisponível");

        Order o = new Order();
        o.setTrackingCode(newTrackingCode());
        o.setComandaNumber(null);
        o.setCustomerName(d.customerName().trim());
        o.setCustomerPhone(d.customerPhone().trim());
        o.setCustomerEmail(d.customerEmail() == null ? null : d.customerEmail().trim());
        o.setStreet(deliveryCalculation.street());
        o.setNumber(d.number().trim());
        o.setComplement(d.complement() == null || d.complement().isBlank() ? null : d.complement().trim());
        o.setNeighborhood(deliveryCalculation.neighborhood());
        o.setCity(deliveryCalculation.city());
        o.setState(deliveryCalculation.state());
        o.setZipCode(deliveryCalculation.zipCode());
        o.setDeliveryZone(zone);
        o.setDeliveryZoneName(zone.getName());
        o.setDeliveryFee(zone.getFee());
        o.setDiscount(BigDecimal.ZERO);
        o.setPaymentMethod(d.paymentMethod());
        o.setNotes(d.notes() == null || d.notes().isBlank() ? null : d.notes().trim());
        o.setStatus(d.paymentMethod() == PaymentMethod.CASH ? OrderStatus.PAYMENT_PROMISED : OrderStatus.PENDING_PAYMENT);
        o.setPaymentConfirmed(false);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest item : d.items()) {
            Product p = products.findById(item.productId()).orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + item.productId()));
            if (!p.isAvailable() || p.getStockQuantity() < item.quantity()) throw new BusinessException("Produto sem estoque/disponível: " + p.getName() + " - " + p.getSize());

            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            OrderItem oi = new OrderItem();
            oi.setProduct(p); oi.setProductName(p.getName()); oi.setSize(p.getSize()); oi.setUnitPrice(p.getPrice()); oi.setQuantity(item.quantity()); oi.setSubtotal(line);
            o.addItem(oi);
            p.setStockQuantity(p.getStockQuantity() - item.quantity());
            if (p.getStockQuantity() == 0) p.setAvailable(false);
            products.save(p);
            subtotal = subtotal.add(line);
        }

        o.setSubtotal(subtotal);
        o.setTotal(subtotal.add(zone.getFee()).subtract(o.getDiscount()));
        o.setComandaNumber(nextComandaNumber());

        Order saved = orders.save(o);
        history.save(new OrderStatusHistory(saved, saved.getStatus(), saved.getCreatedAt()));

        if (saved.getStatus() == OrderStatus.PAID && saved.getCustomerEmail() != null) email.sendOrderConfirmation(saved);
        return to(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse find(Long id) {
        return to(orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> all() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(this::to).toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderPageResponse adminPage(String search, OrderStatus status, Pageable pageable) {
        String normalized = search == null ? "" : search.trim();
        Long comanda = null;
        if (!normalized.isBlank() && normalized.matches("\\d+")) {
            try { comanda = Long.valueOf(normalized); } catch (NumberFormatException ignored) { }
        }
        Page<Order> page = orders.findAdminOrders(normalized, status, comanda, pageable);
        return new AdminOrderPageResponse(page.getContent().stream().map(this::to).toList(), page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order o = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
        if (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CANCELLED) throw new BusinessException("Pedido encerrado não pode ter o status alterado");
        if (!EnumSet.of(OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED).contains(status)) throw new BusinessException("Use as ações específicas para controlar o pagamento ou o cancelamento do pedido");
        if (o.getStatus() == status) return to(o);
        if (o.getStatus() == OrderStatus.PENDING_PAYMENT) throw new BusinessException("O pedido ainda não está liberado para produção");
        o.setStatus(status);
        if (status == OrderStatus.PAID) o.setPaymentConfirmed(true);
        Order saved = orders.save(o);
        history.save(new OrderStatusHistory(saved, status, LocalDateTime.now()));
        if (status == OrderStatus.PAID && saved.getCustomerEmail() != null) email.sendOrderConfirmation(saved);
        return to(saved);
    }

    @Transactional
    public OrderResponse markPaymentOnDeliveryAsPaid(Long id) {
        Order o = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
        if (o.getPaymentMethod() != PaymentMethod.CASH || o.getStatus() != OrderStatus.PAYMENT_PROMISED) throw new BusinessException("Este pedido não está aguardando pagamento na entrega");
        o.setPaymentConfirmed(true); o.setStatus(OrderStatus.PAID);
        Order saved = orders.save(o);
        history.save(new OrderStatusHistory(saved, OrderStatus.PAID, LocalDateTime.now()));
        if (saved.getCustomerEmail() != null) email.sendOrderConfirmation(saved);
        return to(saved);
    }

    @Transactional(readOnly = true)
    public TrackingOrderResponse tracking(String code) {
        Order o = orders.findByTrackingCode(code.trim().toUpperCase()).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        return new TrackingOrderResponse(
                o.getTrackingCode(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getSubtotal(),
                o.getDeliveryFee(),
                o.getTotal(),
                o.getDeliveryZoneName(),
                address(o),
                o.getPaymentMethod(),
                o.isPaymentConfirmed(),
                o.getItems().stream().map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProductName(), i.getSize(), i.getUnitPrice(), i.getQuantity(), i.getSubtotal())).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> statusHistory(Long id) {
        if (!orders.existsById(id)) throw new ResourceNotFoundException("Pedido não encontrado");
        return history.findAllByOrderIdOrderByCreatedAtAsc(id).stream().map(h -> new OrderStatusHistoryResponse(h.getStatus(), h.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> customerHistory(Long id) {
        Order current = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
        return orders.findAllByCustomerPhoneOrderByCreatedAtDesc(current.getCustomerPhone()).stream().filter(o -> !Objects.equals(o.getId(), id)).map(this::to).toList();
    }

    @Transactional
    public void cancelarPagamentoFalho(Long orderId) {
        Order order = orders.findById(orderId).orElse(null);
        if (order == null || order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) return;
        order.setStatus(OrderStatus.CANCELLED); order.setPaymentConfirmed(false); releaseStockIfNeeded(order);
        Order saved = orders.save(order);
        history.save(new OrderStatusHistory(saved, OrderStatus.CANCELLED, LocalDateTime.now()));
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expirarPixPendentes() {
        LocalDateTime now = LocalDateTime.now();
        orders.findAllByStatusAndPaymentMethodAndPaymentExpiresAtLessThanEqualOrderByPaymentExpiresAtAsc(OrderStatus.PENDING_PAYMENT, PaymentMethod.PIX, now)
                .forEach(order -> {
                    if (order.getStatus() != OrderStatus.PENDING_PAYMENT || order.getPaymentMethod() != PaymentMethod.PIX) return;
                    order.setStatus(OrderStatus.CANCELLED); order.setPaymentConfirmed(false); order.setPaymentStatus("expired"); releaseStockIfNeeded(order);
                    Order saved = orders.save(order);
                    history.save(new OrderStatusHistory(saved, OrderStatus.CANCELLED, now));
                });
    }
}
