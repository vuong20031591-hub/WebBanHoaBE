package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.dto.SePayWebhookRequest;

public interface PaymentService {
    PaymentCheckoutResponse generateVietQrCheckout(String userId, Long orderId);

    PaymentCheckoutResponse generateSePayCheckout(String userId, Long orderId);

    PaymentWebhookResult handleVietQrWebhook(PaymentWebhookRequest request, String headerSignature);

    PaymentWebhookResult handleSePayWebhook(
            SePayWebhookRequest request,
            String authorizationHeader,
            String legacyWebhookSecret,
            String clientIp
    );

    PaymentReconciliationResponse reconcileOrderPayments(String userId, Long orderId);

    int syncPendingPayments();
}
