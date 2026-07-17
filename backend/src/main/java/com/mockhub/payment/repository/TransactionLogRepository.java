package com.mockhub.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.payment.entity.TransactionLog;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByOrderId(Long orderId);
}
