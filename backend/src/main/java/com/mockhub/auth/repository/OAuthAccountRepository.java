package com.mockhub.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.auth.entity.OAuthAccount;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderAccountId(String provider, String providerAccountId);

    List<OAuthAccount> findByUserId(Long userId);

    Optional<OAuthAccount> findByUserIdAndProvider(Long userId, String provider);

    boolean existsByUserIdAndProvider(Long userId, String provider);

    void deleteByUserIdAndProvider(Long userId, String provider);

    long countByUserId(Long userId);
}
