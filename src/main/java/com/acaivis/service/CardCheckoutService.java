package com.acaivis.service;

import com.acaivis.dto.OrderItemRequest;
import com.acaivis.dto.OrderRequest;
import com.acaivis.dto.OrderResponse;
import com.acaivis.dto.payment.MercadoPagoCardCheckoutResponse;
import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.acaivis.exception.BusinessException;
import com.acaivis.exception.ResourceNotFoundException;
import com.acaivis.model.DeliveryZone;
import com.acaivis.model.Order;
import com.acaivis.model.OrderStatus;
import com.acaivis.model.Product;
import com.acaivis.repository.DeliveryZoneRepository;
import com.acaivis.repository.OrderRepository;
import com.acaivis.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CardCheckoutService {

    private final DeliveryZoneRepository zones;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final MercadoPagoService mercadoPago;
    private final OrderService orderService;
    private final PaymentAttemptService paymentAttempts;

    public CardCheckoutService(
            DeliveryZoneRepository zones,
            ProductRepository products,
            OrderRepository orders,
            MercadoPagoService mercadoPago,
            OrderService orderService,
            PaymentAttemptService paymentAttempts
    ) {
        this.zones = zones;
        this.products = products;
        this.orders = orders;
        this.mercadoPago = mercadoPago;
        this.orderService = orderService;
    }

    @Transactional
    public MercadoPagoCardCheckoutResponse checkout(
            OrderRequest orderRequest,
            MercadoPagoPaymentRequest paymentRequest
    ) {
        if (orderRequest.paymentMethod() == null
                || (orderRequest.paymentMethod().name().equals("PIX"))
                || (orderRequest.paymentMethod().name().equals("CASH"))) {
            throw new BusinessException("Este endpoint é exclusivo para cartão.");
        }

        DeliveryZone zone = zones.findById(
                orderRequest.deliveryZoneId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Região de entrega não encontrada"
        ));

        if (!zone.isActive()) {
            throw new BusinessException("Região de entrega indisponível");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest item : orderRequest.items()) {
            Product product = products.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto não encontrado: " + item.productId()
                    ));

            if (!product.isAvailable()
                    || product.getStockQuantity() < item.quantity()) {
                throw new BusinessException(
                        "Produto sem estoque/disponível: "
                                + product.getName()
                                + " - "
                                + product.getSize()
                );
            }

            subtotal = subtotal.add(
                    product.getPrice().multiply(
                            BigDecimal.valueOf(item.quantity())
                    )
            );
        }

        BigDecimal total = subtotal
                .add(zone.getFee());

        MercadoPagoPaymentResponse payment = mercadoPago.criarPagamento(
                total,
                "card-checkout-" + System.nanoTime(),
                paymentRequest
        );

        if (!isApproved(payment.status())) {
            if (isProcessing(payment.status())) {
                paymentAttempts.register(orderRequest, total, payment);
            }
            return new MercadoPagoCardCheckoutResponse(payment, null);
        }

        OrderResponse created = orderService.create(orderRequest);

        Order order = orders.findById(created.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido recém-criado não encontrado"
                ));

        order.setMercadoPagoOrderId(payment.orderId());
        order.setMercadoPagoPaymentId(payment.paymentId());
        order.setPaymentStatus(payment.status());
        order.setPaymentStatusDetail(payment.statusDetail());
        order.setPaymentConfirmed(true);
        order.setStatus(OrderStatus.PAID);
        orders.save(order);

        return new MercadoPagoCardCheckoutResponse(
                payment,
                orderService.find(order.getId())
        );
    }

    private boolean isApproved(String status) {
        return "processed".equalsIgnoreCase(status)
                || "approved".equalsIgnoreCase(status)
                || "accredited".equalsIgnoreCase(status);
    }

    private boolean isProcessing(String status) {
        return status == null
                || "created".equalsIgnoreCase(status)
                || "processing".equalsIgnoreCase(status)
                || "pending".equalsIgnoreCase(status)
                || "in_process".equalsIgnoreCase(status)
                || "action_required".equalsIgnoreCase(status);
    }
}
