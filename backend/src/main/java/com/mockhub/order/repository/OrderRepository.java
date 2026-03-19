package com.mockhub.order.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mockhub.order.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderNumber LIKE :prefix%")
    long countByOrderNumberPrefix(@Param("prefix") String prefix);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start")
    long countByCreatedAtAfter(@Param("start") Instant start);
}
