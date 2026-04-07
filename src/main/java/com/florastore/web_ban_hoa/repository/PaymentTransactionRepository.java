package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.entity.PaymentTransaction;
import com.florastore.web_ban_hoa.entity.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByPaymentMethodAndProviderTransactionId(PaymentMethod paymentMethod, String providerTransactionId);

    Optional<PaymentTransaction> findFirstByProviderTransactionIdAndStatusOrderByCreatedAtDesc(
            String providerTransactionId,
            PaymentTransactionStatus status
    );

    Optional<PaymentTransaction> findFirstByProviderTransactionIdOrderByCreatedAtDesc(String providerTransactionId);

    List<PaymentTransaction> findByOrderId(Long orderId);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentTransaction> findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
            Long orderId,
            PaymentMethod paymentMethod,
            PaymentTransactionStatus status
    );

    Optional<PaymentTransaction> findFirstByOrderIdAndPaymentMethodOrderByCreatedAtDesc(
            Long orderId,
            PaymentMethod paymentMethod
    );

    List<PaymentTransaction> findByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
            Long orderId,
            PaymentMethod paymentMethod,
            PaymentTransactionStatus status
    );
}
