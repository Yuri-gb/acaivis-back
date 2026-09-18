package com.acaivis.repository;

import com.acaivis.model.Order;
import com.acaivis.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByStatus(OrderStatus status);
    Optional<Order> findByTrackingCode(String trackingCode);
    Optional<Order> findByMercadoPagoOrderId(String mercadoPagoOrderId);
    List<Order> findAllByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    List<Order> findAllByStatusInOrderByDeliveryRouteOrderAscCreatedAtAsc(Collection<OrderStatus> statuses);

    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
          AND (
            :search = ''
            OR LOWER(o.trackingCode) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR o.customerPhone LIKE CONCAT('%', :search, '%')
            OR (:comandaNumber IS NOT NULL AND o.comandaNumber = :comandaNumber)
          )
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findAdminOrders(
            @Param("search") String search,
            @Param("status") OrderStatus status,
            @Param("comandaNumber") Long comandaNumber,
            Pageable pageable
    );
}
