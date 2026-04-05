package com.florastore.web_ban_hoa.config;

import com.florastore.web_ban_hoa.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentSyncScheduler.class);

    private final PaymentService paymentService;

    public PaymentSyncScheduler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(
            initialDelayString = "#{${payments.polling-initial-delay-seconds:2} * 1000}",
            fixedDelayString = "#{${payments.polling-interval-seconds:5} * 1000}"
    )
    public void syncPendingPayments() {
        int confirmedCount = paymentService.syncPendingPayments();
        if (confirmedCount > 0) {
            log.info("Payment sync confirmed {} pending order(s)", confirmedCount);
        }
    }
}
