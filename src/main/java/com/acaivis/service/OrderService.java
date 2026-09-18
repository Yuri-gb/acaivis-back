
package com.acaivis.service;

import com.acaivis.dto.*;
import com.acaivis.exception.*;
import com.acaivis.model.*;
import com.acaivis.repository.*;
import org.springframework.stereotype.Service;

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
    private final OrderEmailAutomationService emailAutomation;
    private final MercadoPagoService mercadoPago;
    private final DeliveryService delivery;

    public OrderService(
            OrderRepository o,
            ProductRepository p,
            DeliveryZoneRepository z,
            OrderStatusHistoryRepository h,
            OrderEmailAutomationService emailAutomation,
            MercadoPagoService mercadoPago,
            DeliveryService delivery
    ) {
        orders = o;
        products = p;
        zones = z;
        history = h;
        this.emailAutomation = emailAutomation;
        this.mercadoPago = mercadoPago;
        this.delivery = delivery;
    }

    @Transactional
    public OrderResponse create(OrderRequest d) {

        var deliveryCalculation = delivery.calculate(d.zipCode());
        DeliveryZone zone = zones.findById(deliveryCalculation.deliveryZoneId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Região de entrega não encontrada"
                        )
                );

        if (!zone.isActive())
            throw new BusinessException(
                    "Região de entrega indisponível"
            );

        Order o = new Order();

        o.setTrackingCode(newTrackingCode());
        o.setComandaNumber(null);

        o.setCustomerName(d.customerName().trim());
        o.setCustomerPhone(d.customerPhone().trim());

        o.setCustomerEmail(
                d.customerEmail() == null
                        ? null
                        : d.customerEmail().trim()
        );

        o.setStreet(deliveryCalculation.street());
        o.setNumber(d.number().trim());

        o.setComplement(
                d.complement() == null || d.complement().isBlank()
                        ? null
                        : d.complement().trim()
        );

        o.setNeighborhood(deliveryCalculation.neighborhood());
        o.setCity(deliveryCalculation.city());
        o.setState(deliveryCalculation.state());
        o.setZipCode(deliveryCalculation.zipCode());

        o.setDeliveryZone(zone);
        o.setDeliveryZoneName(zone.getName());
        o.setDeliveryFee(zone.getFee());

        o.setDiscount(BigDecimal.ZERO);
        o.setPaymentMethod(d.paymentMethod());

        o.setNotes(
                d.notes() == null || d.notes().isBlank()
                        ? null
                        : d.notes().trim()
        );

        o.setStatus(
                d.paymentMethod() == PaymentMethod.CASH
                        ? OrderStatus.PAYMENT_PROMISED
                        : OrderStatus.PENDING_PAYMENT
        );

        o.setPaymentConfirmed(false);

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest item : d.items()) {

            Product p = products.findById(item.productId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Produto não encontrado: "
                                            + item.productId()
                            )
                    );

            if (!p.isAvailable()
                    || p.getStockQuantity() < item.quantity()) {

                throw new BusinessException(
                        "Produto sem estoque/disponível: "
                                + p.getName()
                                + " - "
                                + p.getSize()
                );
            }

            BigDecimal line = p.getPrice()
                    .multiply(
                            BigDecimal.valueOf(item.quantity())
                    );

            OrderItem oi = new OrderItem();

            oi.setProduct(p);
            oi.setProductName(p.getName());
            oi.setSize(p.getSize());
            oi.setUnitPrice(p.getPrice());
            oi.setQuantity(item.quantity());
            oi.setSubtotal(line);

            o.addItem(oi);

            p.setStockQuantity(
                    p.getStockQuantity() - item.quantity()
            );

            if (p.getStockQuantity() == 0)
                p.setAvailable(false);

            products.save(p);

            subtotal = subtotal.add(line);
        }

        o.setSubtotal(subtotal);

        o.setTotal(
                subtotal
                        .add(zone.getFee())
                        .subtract(o.getDiscount())
        );

        o.setComandaNumber(nextComandaNumber());

        Order saved = orders.save(o);

        history.save(
                new OrderStatusHistory(
                        saved,
                        saved.getStatus(),
                        saved.getCreatedAt()
                )
        );

        if (saved.getStatus() == OrderStatus.PENDING_PAYMENT
                && saved.getPaymentMethod() == PaymentMethod.PIX)
            emailAutomation.notifyPaymentPending(saved);

        return to(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse find(Long id) {
        return to(
                orders.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Pedido não encontrado: " + id
                                )
                        )
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> all() {
        return orders
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::to)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderPageResponse adminPage(
            String search,
            OrderStatus status,
            Pageable pageable
    ) {

        String normalized =
                search == null ? "" : search.trim();

        Long comanda = null;

        if (!normalized.isBlank()
                && normalized.matches("\\d+")) {

            try {
                comanda = Long.valueOf(normalized);
            } catch (NumberFormatException ignored) {
            }
        }

        Page<Order> page =
                orders.findAdminOrders(
                        normalized,
                        status,
                        comanda,
                        pageable
                );

        return new AdminOrderPageResponse(
                page.getContent()
                        .stream()
                        .map(this::to)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    @Transactional
    public OrderResponse updateStatus(
            Long id,
            OrderStatus status
    ) {

        Order o = orders.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido não encontrado: " + id
                        )
                );

        if (o.getStatus() == OrderStatus.DELIVERED
                || o.getStatus() == OrderStatus.CANCELLED) {

            throw new BusinessException(
                    "Pedido encerrado não pode ter o status alterado"
            );
        }

        if (!EnumSet.of(OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED).contains(status)) {
            throw new BusinessException("Use as ações específicas para controlar o pagamento ou o cancelamento do pedido");
        }

        if (o.getStatus() == status)
            return to(o);

        if (o.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("O pedido ainda não está liberado para produção");
        }

        o.setStatus(status);

        if (status == OrderStatus.PAID)
            o.setPaymentConfirmed(true);

        Order saved = orders.save(o);

        history.save(
                new OrderStatusHistory(
                        saved,
                        status,
                        LocalDateTime.now()
                )
        );

        if (status == OrderStatus.PAID)
            emailAutomation.notifyPaymentConfirmed(saved);
        else
            emailAutomation.notifyStatus(saved);

        return to(saved);
    }

    @Transactional
    public OrderResponse markPaymentOnDeliveryAsPaid(Long id) {
        Order o = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
        if (o.getPaymentMethod() != PaymentMethod.CASH || o.getStatus() != OrderStatus.PAYMENT_PROMISED) {
            throw new BusinessException("Este pedido não está aguardando pagamento na entrega");
        }
        o.setPaymentConfirmed(true);
        o.setStatus(OrderStatus.PAID);
        Order saved = orders.save(o);
        history.save(new OrderStatusHistory(saved, OrderStatus.PAID, LocalDateTime.now()));
        if (saved.getCustomerEmail() != null) emailAutomation.notifyPaymentConfirmed(saved);
        return to(saved);
    }

    @Transactional(readOnly = true)
    public TrackingOrderResponse tracking(String code) {

        Order o = orders.findByTrackingCode(
                        code.trim().toUpperCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido não encontrado"
                        )
                );

        return new TrackingOrderResponse(
                o.getTrackingCode(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getSubtotal(),
                o.getDeliveryFee(),
                o.getTotal(),
                o.getDeliveryZoneName(),
                address(o),
                o.getItems()
                        .stream()
                        .map(i ->
                                new OrderItemResponse(
                                        i.getProduct().getId(),
                                        i.getProductName(),
                                        i.getSize(),
                                        i.getUnitPrice(),
                                        i.getQuantity(),
                                        i.getSubtotal()
                                )
                        )
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> statusHistory(Long id) {

        if (!orders.existsById(id))
            throw new ResourceNotFoundException(
                    "Pedido não encontrado: " + id
            );

        return history
                .findAllByOrderIdOrderByCreatedAtAsc(id)
                .stream()
                .map(h ->
                        new OrderStatusHistoryResponse(
                                h.getStatus(),
                                h.getCreatedAt()
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> customerHistory(Long id) {

        Order current = orders.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido não encontrado: " + id
                        )
                );

        return orders
                .findAllByCustomerPhoneOrderByCreatedAtDesc(
                        current.getCustomerPhone()
                )
                .stream()
                .filter(o ->
                        !Objects.equals(o.getId(), id)
                )
                .map(this::to)
                .toList();
    }

    private Long nextComandaNumber() {

        return ((Number)
                ordersEntityManager()
                        .createNativeQuery(
                                "SELECT nextval('order_comanda_seq')"
                        )
                        .getSingleResult()
        ).longValue();
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private jakarta.persistence.EntityManager ordersEntityManager() {
        return em;
    }

    private String newTrackingCode() {

        String code;

        do {

            StringBuilder b =
                    new StringBuilder("AC-");

            for (int i = 0; i < 6; i++)
                b.append(
                        TRACK_CHARS[
                                random.nextInt(
                                        TRACK_CHARS.length
                                )
                        ]
                );

            code = b.toString();

        } while (
                orders.findByTrackingCode(code).isPresent()
        );

        return code;
    }

    private String address(Order o) {

        return o.getStreet()
                + ", "
                + o.getNumber()
                + (
                    o.getComplement() != null
                            ? " - " + o.getComplement()
                            : ""
                )
                + ", "
                + o.getNeighborhood()
                + " - "
                + o.getCity()
                + "/"
                + o.getState()
                + ", "
                + o.getZipCode();
    }

    private OrderResponse to(Order o) {

        return new OrderResponse(
                o.getId(),
                o.getTrackingCode(),
                o.getComandaNumber(),
                o.getCustomerName(),
                o.getCustomerPhone(),
                o.getCustomerEmail(),
                address(o),
                o.getStreet(),
                o.getNumber(),
                o.getComplement(),
                o.getNeighborhood(),
                o.getCity(),
                o.getState(),
                o.getZipCode(),
                o.getDeliveryZone().getId(),
                o.getDeliveryZoneName(),
                o.getSubtotal(),
                o.getDeliveryFee(),
                o.getDiscount(),
                o.getTotal(),
                o.getPaymentMethod(),
                o.isPaymentConfirmed(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getNotes(),
                o.getItems()
                        .stream()
                        .map(i ->
                                new OrderItemResponse(
                                        i.getProduct().getId(),
                                        i.getProductName(),
                                        i.getSize(),
                                        i.getUnitPrice(),
                                        i.getQuantity(),
                                        i.getSubtotal()
                                )
                        )
                        .toList(),
                o.getDeliveryRouteOrder(),
                o.getDeliveryProofUrl(),
                o.getDeliveredAt(),
                o.getDeliveredBy()
        );
    }

}
