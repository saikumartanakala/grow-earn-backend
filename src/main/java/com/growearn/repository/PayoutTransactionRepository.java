package com.growearn.repository;

import com.growearn.entity.PayoutTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, Long> {
    Optional<PayoutTransaction> findByRazorpayPayoutId(String razorpayPayoutId);
    Optional<PayoutTransaction> findByWithdrawalId(Long withdrawalId);
}
