package com.mockhub.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.mockhub.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.listing l JOIN FETCH l.event e JOIN FETCH e.venue WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByPaymentIntentId(String paymentIntentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.paymentIntentId = :paymentIntentId")
    Optional<Order> findByPaymentIntentIdForUpdate(@Param("paymentIntentId") String paymentIntentId);

    @Query("SELECT MAX(o.orderNumber) FROM Order o WHERE o.orderNumber LIKE :prefix%")
    Optional<String> findMaxOrderNumberByPrefix(@Param("prefix") String prefix);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start")
    long countByCreatedAtAfter(@Param("start") Instant start);

    /**
     * Orders left pending past the abandonment cutoff. Their tickets are still held, so an
     * agent that creates a checkout and walks away keeps seats out of circulation until
     * these orders are failed and their inventory released.
     */
    @Query("SELECT o FROM Order o WHERE o.status = com.mockhub.order.entity.OrderStatus.PENDING "
            + "AND o.createdAt < :cutoff ORDER BY o.createdAt ASC")
    List<Order> findAbandonedPendingOrders(@Param("cutoff") Instant cutoff, Pageable pageable);
}
