package com.florastore.web_ban_hoa.dto;

import java.util.List;

public record PaymentReconciliationResponse(
        Long orderId,
        String orderStatus,
        int transactionCount,
        boolean paid,
        List<String> transactions
) {
}
