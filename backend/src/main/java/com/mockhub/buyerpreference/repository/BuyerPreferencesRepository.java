package com.mockhub.buyerpreference.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.buyerpreference.entity.BuyerPreferences;

public interface BuyerPreferencesRepository extends JpaRepository<BuyerPreferences, Long> {

    Optional<BuyerPreferences> findByUserId(Long userId);
}
