package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;

public interface PaymentService {
    PaymentCheckoutResponse generateVietQrCheckout(Long orderId);

    PaymentCheckoutResponse generateSePayCheckout(Long orderId);

    PaymentWebhookResult handleVietQrWebhook(PaymentWebhookRequest request, String headerSignature);

    PaymentWebhookResult handleSePayWebhook(PaymentWebhookRequest request, String headerSignature, String headerSecret);

    PaymentReconciliationResponse reconcileOrderPayments(Long orderId);
}
