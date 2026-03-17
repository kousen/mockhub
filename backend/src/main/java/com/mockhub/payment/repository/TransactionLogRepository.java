package com.mockhub.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.payment.entity.TransactionLog;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByOrderId(Long orderId);
}
