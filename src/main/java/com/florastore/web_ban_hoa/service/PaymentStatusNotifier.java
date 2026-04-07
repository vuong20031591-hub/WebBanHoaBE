package com.florastore.web_ban_hoa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PaymentStatusNotifier {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusNotifier.class);
    private static final long EMITTER_TIMEOUT_MS = 15 * 60 * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByOrderId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long orderId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByOrderId.computeIfAbsent(orderId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeEmitter(orderId, emitter));
        emitter.onError(ex -> removeEmitter(orderId, emitter));

        sendEvent(orderId, emitter, "CONNECTED", String.valueOf(orderId));
        return emitter;
    }

    public void publishPaid(Long orderId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByOrderId.get(orderId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            sendEvent(orderId, emitter, "PAID", String.valueOf(orderId));
        }
    }

    private void sendEvent(Long orderId, SseEmitter emitter, String status, String orderValue) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name("payment-status")
                            .data("{\"status\":\"" + status + "\",\"orderId\":" + orderValue + "}")
            );
        } catch (IOException ex) {
            log.debug("Cannot send payment SSE event for order {}: {}", orderId, ex.getMessage());
            removeEmitter(orderId, emitter);
        }
    }

    private void removeEmitter(Long orderId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByOrderId.get(orderId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByOrderId.remove(orderId);
        }
    }
}
