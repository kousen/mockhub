package com.mockhub.order.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mockhub.order.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            SELECT oi FROM OrderItem oi
            JOIN FETCH oi.order o
            JOIN FETCH oi.listing l
            JOIN FETCH l.event e
            JOIN FETCH oi.ticket t
            JOIN FETCH t.section s
            LEFT JOIN FETCH t.seat seat
            LEFT JOIN FETCH seat.row r
            WHERE l.seller.id = :sellerId AND o.status = 'COMPLETED'
            ORDER BY o.confirmedAt DESC
            """)
    List<OrderItem> findCompletedSalesBySellerId(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COALESCE(SUM(oi.pricePaid), 0)
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.listing l
            WHERE l.seller.id = :sellerId AND o.status = 'COMPLETED'
            """)
    BigDecimal sumEarningsBySellerId(@Param("sellerId") Long sellerId);
}
