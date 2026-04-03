package com.florastore.web_ban_hoa.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.dto.SePayWebhookRequest;
import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.entity.PaymentTransaction;
import com.florastore.web_ban_hoa.entity.PaymentTransactionStatus;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.repository.PaymentTransactionRepository;
import com.florastore.web_ban_hoa.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final char[] RANDOM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(?i)(?<![A-Z0-9])(QRD[A-Z0-9]{7,})(?![A-Z0-9])");

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestClient restClient;

    private final String vietqrApiBaseUrl;
    private final String vietqrClientId;
    private final String vietqrApiKey;
    private final String vietqrWebhookSecret;
    private final String vietqrSigningSecret;

    private final String sepayApiBaseUrl;
    private final String sepayApiToken;
    private final String sepayWebhookSecret;
    private final String sepaySigningSecret;

    private final String bankAccountExpected;
    private final String bankAccountNumber;
    private final String bankAccountName;
    private final String bankName;
    private final String bankBin;

    private final int qrExpiryMinutes;
    private final BigDecimal paymentAmountTolerance;
    private final Set<String> webhookIpWhitelist;

    public PaymentServiceImpl(
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            @Value("${payments.vietqr.api-base-url:https://api.vietqr.io}") String vietqrApiBaseUrl,
            @Value("${payments.vietqr.client-id:}") String vietqrClientId,
            @Value("${payments.vietqr.api-key:}") String vietqrApiKey,
            @Value("${payments.vietqr.webhook-secret:}") String vietqrWebhookSecret,
            @Value("${payments.vietqr.signing-secret:}") String vietqrSigningSecret,
            @Value("${payments.sepay.api-base-url:https://my.sepay.vn/userapi}") String sepayApiBaseUrl,
            @Value("${payments.sepay.api-token:}") String sepayApiToken,
            @Value("${payments.sepay.webhook-secret:}") String sepayWebhookSecret,
            @Value("${payments.sepay.signing-secret:}") String sepaySigningSecret,
            @Value("${payments.bank.account-expected:}") String bankAccountExpected,
            @Value("${payments.bank.account-number:}") String bankAccountNumber,
            @Value("${payments.bank.account-name:}") String bankAccountName,
            @Value("${payments.bank.name:}") String bankName,
            @Value("${payments.bank.bin:}") String bankBin,
            @Value("${payments.qr.expiry-minutes:15}") int qrExpiryMinutes,
            @Value("${payments.amount-tolerance:0}") BigDecimal paymentAmountTolerance,
            @Value("${payments.webhook.ip-whitelist:}") String webhookIpWhitelist
    ) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.restClient = RestClient.builder().build();
        this.vietqrApiBaseUrl = trimTrailingSlash(vietqrApiBaseUrl);
        this.vietqrClientId = vietqrClientId;
        this.vietqrApiKey = vietqrApiKey;
        this.vietqrWebhookSecret = vietqrWebhookSecret;
        this.vietqrSigningSecret = vietqrSigningSecret;
        this.sepayApiBaseUrl = trimTrailingSlash(sepayApiBaseUrl);
        this.sepayApiToken = sepayApiToken;
        this.sepayWebhookSecret = sepayWebhookSecret;
        this.sepaySigningSecret = sepaySigningSecret;
        this.bankAccountExpected = bankAccountExpected;
        this.bankAccountNumber = bankAccountNumber;
        this.bankAccountName = bankAccountName;
        this.bankName = bankName;
        this.bankBin = bankBin;
        this.qrExpiryMinutes = qrExpiryMinutes;
        this.paymentAmountTolerance = paymentAmountTolerance != null ? paymentAmountTolerance.abs() : BigDecimal.ZERO;
        this.webhookIpWhitelist = parseIpWhitelist(webhookIpWhitelist);
    }

    @Override
    public PaymentCheckoutResponse generateVietQrCheckout(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        validateCheckoutOrder(order, PaymentMethod.VIETQR);
        return buildCheckout(order, PaymentMethod.VIETQR);
    }

    @Override
    public PaymentCheckoutResponse generateSePayCheckout(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        validateCheckoutOrder(order, PaymentMethod.SEPAY);
        return buildCheckout(order, PaymentMethod.SEPAY);
    }

    @Override
    public PaymentWebhookResult handleVietQrWebhook(PaymentWebhookRequest request, String headerSignature) {
        log.info(
                "[VIETQR WEBHOOK] orderId={} providerTransactionId={} amount={}",
                request.orderId(),
                request.providerTransactionId(),
                request.amount()
        );
        validateSignature(request, headerSignature, vietqrSigningSecret);
        validateHeaderSecret(request.secret(), vietqrWebhookSecret);
        return processWebhook(PaymentMethod.VIETQR, request);
    }

    @Override
    public PaymentWebhookResult handleSePayWebhook(
            SePayWebhookRequest request,
            String authorizationHeader,
            String legacyWebhookSecret,
            String clientIp
    ) {
        log.info(
                "[SEPAY WEBHOOK] id={} accountNumber={} amount={} code={} clientIp={}",
                request.id(),
                request.accountNumber(),
                request.transferAmount(),
                request.code(),
                clientIp
        );
        validateWebhookIp(clientIp);
        validateSePayAuthorization(authorizationHeader, legacyWebhookSecret);

        if (!"in".equalsIgnoreCase(request.transferType())) {
            return new PaymentWebhookResult("IGNORED", "Outbound transaction ignored", null, String.valueOf(request.id()));
        }

        String expectedAccount = firstNonBlank(bankAccountExpected, bankAccountNumber);
        if (!isBlank(expectedAccount) && !expectedAccount.equals(request.accountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook account number does not match configured bank account");
        }

        String paymentCode = extractPaymentCode(request.code(), request.content(), request.description(), request.referenceCode());
        if (isBlank(paymentCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment code not found in SePay webhook");
        }

        PaymentTransaction pendingTransaction = paymentTransactionRepository
                .findFirstByProviderTransactionIdAndStatusOrderByCreatedAtDesc(paymentCode, PaymentTransactionStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending payment transaction not found"));

        Order order = pendingTransaction.getOrder();
        if (order.getStatus() != OrderStatus.PENDING) {
            return new PaymentWebhookResult("IGNORED", "Order already processed", order.getId(), String.valueOf(request.id()));
        }
        if (pendingTransaction.getPaymentMethod() == PaymentMethod.COD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COD order cannot be confirmed by SePay webhook");
        }
        if (isExpired(pendingTransaction)) {
            pendingTransaction.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransactionRepository.save(pendingTransaction);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code expired");
        }
        if (!isInboundAmountMatch(order.getTotalAmount(), request.transferAmount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook amount does not match order amount");
        }

        return markPaymentAsSuccess(order, pendingTransaction, request.transferAmount(), String.valueOf(request.id()), "Webhook processed");
    }

    @Override
    public PaymentReconciliationResponse reconcileOrderPayments(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        syncBankTransferIfPossible(order);

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrderId(orderId);
        List<String> txSummary = transactions.stream()
                .map(tx -> tx.getPaymentMethod().name() + ":" + tx.getProviderTransactionId() + ":" + tx.getStatus().name())
                .toList();

        boolean paid = transactions.stream().anyMatch(tx -> tx.getStatus() == PaymentTransactionStatus.SUCCESS);
        return new PaymentReconciliationResponse(order.getId(), order.getStatus().name(), transactions.size(), paid, txSummary);
    }

    @Override
    public int syncPendingPayments() {
        int confirmedCount = 0;
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        for (Order order : pendingOrders) {
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                continue;
            }
            try {
                syncBankTransferIfPossible(order);
                if (order.getStatus() == OrderStatus.CONFIRMED) {
                    confirmedCount++;
                }
            } catch (Exception ex) {
                log.warn("Payment sync skipped for order {}: {}", order.getId(), ex.getMessage());
            }
        }
        return confirmedCount;
    }

    private PaymentCheckoutResponse buildCheckout(Order order, PaymentMethod provider) {
        PaymentTransaction pendingTransaction = getOrCreatePendingTransaction(order, provider);
        String transferCode = pendingTransaction.getProviderTransactionId();
        LocalDateTime expiresAt = pendingTransaction.getCreatedAt().plusMinutes(qrExpiryMinutes);
        long expiresInSeconds = Math.max(0, java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds());
        GeneratedQr generatedQr = tryGenerateHostedQr(order, provider, transferCode);
        if (generatedQr != null) {
            return new PaymentCheckoutResponse(
                    order.getId(),
                    provider.name(),
                    generatedQr.checkoutUrl(),
                    generatedQr.qrContent(),
                    generatedQr.note(),
                    transferCode,
                    expiresAt,
                    expiresInSeconds
            );
        }

        if (provider == PaymentMethod.VIETQR) {
            String qrContent = "VIETQR|ORDER=" + order.getId() + "|AMOUNT=" + order.getTotalAmount();
            String checkoutUrl = vietqrApiBaseUrl + "/checkout?orderId=" + order.getId();
            return new PaymentCheckoutResponse(order.getId(), "VIETQR", checkoutUrl, qrContent, "Fallback QR payload for local testing", transferCode, expiresAt, expiresInSeconds);
        }

        String checkoutUrl = sepayApiBaseUrl + "/checkout?orderId=" + order.getId() + "&amount=" + order.getTotalAmount();
        return new PaymentCheckoutResponse(order.getId(), "SEPAY", checkoutUrl, null, "Fallback checkout URL for local testing", transferCode, expiresAt, expiresInSeconds);
    }

    private PaymentTransaction getOrCreatePendingTransaction(Order order, PaymentMethod provider) {
        PaymentTransaction existing = paymentTransactionRepository
                .findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(order.getId(), provider, PaymentTransactionStatus.PENDING)
                .orElse(null);

        if (existing != null) {
            if (!isExpired(existing)) {
                if (existing.getAmount().compareTo(order.getTotalAmount()) != 0) {
                    existing.setAmount(order.getTotalAmount());
                    paymentTransactionRepository.save(existing);
                }
                return existing;
            }
            existing.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransactionRepository.save(existing);
        }

        PaymentTransaction pending = new PaymentTransaction(
                order,
                provider,
                generateTransactionId(order.getId()),
                order.getTotalAmount(),
                PaymentTransactionStatus.PENDING
        );
        return paymentTransactionRepository.save(pending);
    }

    private GeneratedQr tryGenerateHostedQr(Order order, PaymentMethod provider, String transferCode) {
        if (!canGenerateRealQr()) {
            return null;
        }

        Integer acqId = parseBankBin();
        if (acqId == null) {
            return null;
        }

        VietQrGenerateRequest request = new VietQrGenerateRequest(
                bankAccountNumber,
                bankAccountName,
                acqId,
                order.getTotalAmount().setScale(0, RoundingMode.HALF_UP),
                transferCode,
                "compact2",
                "text"
        );

        try {
            VietQrGenerateResponse response = restClient.post()
                    .uri(vietqrApiBaseUrl + "/v2/generate")
                    .header("x-client-id", vietqrClientId)
                    .header("x-api-key", vietqrApiKey)
                    .body(request)
                    .retrieve()
                    .body(VietQrGenerateResponse.class);

            if (response == null || !"00".equals(response.code()) || response.data() == null || isBlank(response.data().qrCode())) {
                log.warn("VietQR returned invalid payload for order {} provider {}", order.getId(), provider);
                return null;
            }

            String checkoutUrl = firstNonBlank(response.data().qrDataURL(), response.data().qrLink(), vietqrApiBaseUrl);
            String note = buildCheckoutNote(provider, transferCode);
            return new GeneratedQr(checkoutUrl, response.data().qrCode(), note);
        } catch (Exception ex) {
            log.warn("Failed to generate VietQR payload for order {} provider {}. Falling back to local checkout payload.", order.getId(), provider, ex);
            return null;
        }
    }

    private void syncBankTransferIfPossible(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.COD || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        boolean hasSuccessfulTransaction = paymentTransactionRepository.findByOrderId(order.getId()).stream()
                .anyMatch(tx -> tx.getStatus() == PaymentTransactionStatus.SUCCESS);
        if (hasSuccessfulTransaction) {
            return;
        }

        PaymentTransaction pendingTransaction = paymentTransactionRepository
                .findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(order.getId(), order.getPaymentMethod(), PaymentTransactionStatus.PENDING)
                .orElse(null);
        if (pendingTransaction == null || isExpired(pendingTransaction)) {
            if (pendingTransaction != null && pendingTransaction.getStatus() == PaymentTransactionStatus.PENDING) {
                pendingTransaction.setStatus(PaymentTransactionStatus.FAILED);
                paymentTransactionRepository.save(pendingTransaction);
            }
            return;
        }

        SePayTransaction matched = findMatchingSePayTransaction(order, pendingTransaction);
        if (matched == null) {
            return;
        }

        markPaymentAsSuccess(order, pendingTransaction, matched.amount(), matched.referenceId(), "Payment reconciled");
    }

    private SePayTransaction findMatchingSePayTransaction(Order order, PaymentTransaction pendingTransaction) {
        if (isBlank(sepayApiToken) || isBlank(sepayApiBaseUrl)) {
            return null;
        }

        String expectedAccount = firstNonBlank(bankAccountExpected, bankAccountNumber);
        if (isBlank(expectedAccount)) {
            return null;
        }

        String minDate = order.getCreatedAt().toLocalDate().minusDays(1).format(DATE_FORMATTER);
        String maxDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);

        try {
            String pendingCode = pendingTransaction.getProviderTransactionId().toLowerCase(Locale.ROOT);
            int page = 1;
            int limit = 100;

            while (page <= 10) {
                String url = sepayApiBaseUrl
                        + "/transactions/list?limit=" + limit
                        + "&page=" + page
                        + "&account_number=" + UriUtils.encodeQueryParam(expectedAccount, StandardCharsets.UTF_8)
                        + "&amount_in=" + UriUtils.encodeQueryParam(order.getTotalAmount().stripTrailingZeros().toPlainString(), StandardCharsets.UTF_8)
                        + "&transaction_date_min=" + UriUtils.encodeQueryParam(minDate, StandardCharsets.UTF_8)
                        + "&transaction_date_max=" + UriUtils.encodeQueryParam(maxDate, StandardCharsets.UTF_8);

                SePayTransactionsResponse response = restClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + sepayApiToken)
                        .retrieve()
                        .body(SePayTransactionsResponse.class);

                List<SePayTransactionItem> transactions = extractTransactions(response);
                if (transactions.isEmpty()) {
                    return null;
                }

                SePayTransaction matched = transactions.stream()
                        .filter(tx -> expectedAccount.equals(tx.accountNumber()))
                        .filter(tx -> isInboundAmountMatch(order.getTotalAmount(), tx.amountIn()))
                        .filter(tx -> matchesPaymentCode(tx, pendingCode))
                        .findFirst()
                        .map(tx -> new SePayTransaction(
                                tx.id() != null ? tx.id() : firstNonBlank(tx.referenceNumber(), pendingTransaction.getProviderTransactionId()),
                                parseAmount(tx.amountIn())
                        ))
                        .orElse(null);

                if (matched != null) {
                    return matched;
                }

                if (!hasMorePages(response, page, limit, transactions.size())) {
                    return null;
                }

                page++;
            }

            return null;
        } catch (Exception ex) {
            log.warn("SePay transaction sync skipped: {}", ex.getMessage());
            return null;
        }
    }

    private boolean matchesPaymentCode(SePayTransactionItem tx, String transferCode) {
        if (!isBlank(tx.code())) {
            return tx.code().equalsIgnoreCase(transferCode);
        }

        return containsPaymentCodeToken(tx.transactionContent(), transferCode)
                || containsPaymentCodeToken(tx.referenceNumber(), transferCode)
                || containsPaymentCodeToken(tx.description(), transferCode);
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value != null && expected != null && value.toLowerCase(Locale.ROOT).contains(expected);
    }

    private boolean containsPaymentCodeToken(String value, String expectedCode) {
        if (isBlank(value) || isBlank(expectedCode)) {
            return false;
        }

        Matcher matcher = PAYMENT_CODE_PATTERN.matcher(value.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            if (expectedCode.equalsIgnoreCase(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private boolean isInboundAmountMatch(BigDecimal orderAmount, String amountIn) {
        BigDecimal received = parseAmount(amountIn);
        return isInboundAmountMatch(orderAmount, received);
    }

    private boolean isInboundAmountMatch(BigDecimal orderAmount, BigDecimal received) {
        if (received == null) {
            return false;
        }
        return orderAmount.subtract(received).abs().compareTo(paymentAmountTolerance) <= 0;
    }

    private BigDecimal parseAmount(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PaymentWebhookResult processWebhook(PaymentMethod method, PaymentWebhookRequest request) {
        Order order = getOrderOrThrow(request.orderId());
        if (order.getPaymentMethod() != method) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payment method does not match order");
        }

        if (order.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook amount does not match order amount");
        }

        PaymentTransaction pendingTransaction = paymentTransactionRepository
                .findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(order.getId(), method, PaymentTransactionStatus.PENDING)
                .orElse(null);

        if (pendingTransaction == null) {
            return new PaymentWebhookResult("IGNORED", "Transaction already processed", order.getId(), request.providerTransactionId());
        }

        if (isExpired(pendingTransaction)) {
            pendingTransaction.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransactionRepository.save(pendingTransaction);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code expired");
        }

        if (!containsIgnoreCase(request.transactionContent(), pendingTransaction.getProviderTransactionId().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment content does not match payment code");
        }

        pendingTransaction.setStatus(PaymentTransactionStatus.SUCCESS);
        pendingTransaction.setAmount(request.amount());
        try {
            paymentTransactionRepository.saveAndFlush(pendingTransaction);
        } catch (DataIntegrityViolationException ex) {
            return new PaymentWebhookResult("IGNORED", "Transaction already processed", order.getId(), request.providerTransactionId());
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        return new PaymentWebhookResult("OK", "Webhook processed", order.getId(), request.providerTransactionId());
    }

    private PaymentWebhookResult markPaymentAsSuccess(
            Order order,
            PaymentTransaction pendingTransaction,
            BigDecimal amount,
            String providerReference,
            String successMessage
    ) {
        pendingTransaction.setStatus(PaymentTransactionStatus.SUCCESS);
        pendingTransaction.setAmount(amount);
        try {
            paymentTransactionRepository.saveAndFlush(pendingTransaction);
        } catch (DataIntegrityViolationException ex) {
            return new PaymentWebhookResult("IGNORED", "Transaction already processed", order.getId(), providerReference);
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        return new PaymentWebhookResult("OK", successMessage, order.getId(), providerReference);
    }

    private void validateWebhookIp(String clientIp) {
        if (webhookIpWhitelist.isEmpty()) {
            return;
        }

        String normalizedIp = normalizeIp(clientIp);
        if (normalizedIp == null || !webhookIpWhitelist.contains(normalizedIp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook IP is not allowed");
        }
    }

    private void validateSePayAuthorization(String authorizationHeader, String legacyWebhookSecret) {
        if (!isBlank(sepayWebhookSecret)) {
            String expectedHeader = "apikey " + sepayWebhookSecret.trim().toLowerCase(Locale.ROOT);
            if (!isBlank(authorizationHeader) && authorizationHeader.trim().toLowerCase(Locale.ROOT).equals(expectedHeader)) {
                return;
            }

            if (!isBlank(legacyWebhookSecret) && legacyWebhookSecret.trim().equals(sepayWebhookSecret.trim())) {
                return;
            }

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SePay webhook authorization");
        }

        if (isBlank(authorizationHeader) && isBlank(legacyWebhookSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SePay webhook authorization is not configured");
        }
    }

    private String extractPaymentCode(String... values) {
        for (String value : values) {
            String exactCode = normalizePaymentCode(value);
            if (exactCode != null) {
                return exactCode;
            }

            if (isBlank(value)) {
                continue;
            }

            Matcher matcher = PAYMENT_CODE_PATTERN.matcher(value.toUpperCase(Locale.ROOT));
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private String normalizePaymentCode(String value) {
        if (isBlank(value)) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("^QRD[A-Z0-9]{7,}$") ? normalized : null;
    }

    private void validateHeaderSecret(String provided, String configured) {
        if (configured == null || configured.isBlank()) {
            return;
        }
        if (provided == null || !configured.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
        }
    }

    private void validateSignature(PaymentWebhookRequest request, String headerSignature, String signingSecret) {
        if (signingSecret == null || signingSecret.isBlank()) {
            return;
        }
        String providedSignature = headerSignature;
        if ((providedSignature == null || providedSignature.isBlank()) && request.signature() != null) {
            providedSignature = request.signature();
        }
        if (providedSignature == null || providedSignature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signature");
        }

        String payload = request.providerTransactionId() + "|" + request.orderId() + "|" + request.amount();
        String expected = hmacSha256Hex(signingSecret, payload);
        if (!expected.equalsIgnoreCase(providedSignature.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot verify signature", ex);
        }
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private Order getOwnedOrderOrThrow(String userId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void validateCheckoutOrder(Order order, PaymentMethod provider) {
        if (order.getPaymentMethod() != provider) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order payment method does not match checkout provider");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING order can start checkout");
        }
    }

    private boolean canGenerateRealQr() {
        return !isBlank(vietqrClientId)
                && !isBlank(vietqrApiKey)
                && !isBlank(bankAccountNumber)
                && !isBlank(bankAccountName)
                && !isBlank(bankBin);
    }

    private Integer parseBankBin() {
        if (isBlank(bankBin)) {
            return null;
        }
        try {
            return Integer.parseInt(bankBin.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String generateTransactionId(Long orderId) {
        String orderSuffix = String.format("%04d", orderId % 10000);
        String timestampBase36 = Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        String random = randomCode(4);
        return "QRD" + orderSuffix + timestampBase36 + random;
    }

    private String buildCheckoutNote(PaymentMethod provider, String transferCode) {
        String providerNote = provider == PaymentMethod.SEPAY
                ? "SePay can auto-confirm after bank transfer sync."
                : "Use VietQR-compatible banking app to scan and pay.";
        String bankLabel = isBlank(bankName) ? bankAccountName : bankName + " - " + bankAccountName;
        return providerNote + " Transfer content: " + transferCode + ". Account: " + bankAccountNumber + " (" + bankLabel + "). QR expires in " + qrExpiryMinutes + " minutes.";
    }

    private boolean isExpired(PaymentTransaction transaction) {
        return transaction.getCreatedAt() != null && transaction.getCreatedAt().plusMinutes(qrExpiryMinutes).isBefore(LocalDateTime.now());
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM_CHARS[SECURE_RANDOM.nextInt(RANDOM_CHARS.length)]);
        }
        return builder.toString();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private Set<String> parseIpWhitelist(String whitelist) {
        if (isBlank(whitelist)) {
            return Set.of();
        }

        return Arrays.stream(whitelist.split(","))
                .map(this::normalizeIp)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String normalizeIp(String ip) {
        if (isBlank(ip)) {
            return null;
        }

        String normalized = ip.trim();
        if (normalized.startsWith("::ffff:")) {
            normalized = normalized.substring("::ffff:".length());
        }
        return normalized;
    }

    private List<SePayTransactionItem> extractTransactions(SePayTransactionsResponse response) {
        if (response == null) {
            return List.of();
        }
        if (response.transactions() != null) {
            return response.transactions();
        }
        if (response.data() != null) {
            return response.data();
        }
        return List.of();
    }

    private boolean hasMorePages(SePayTransactionsResponse response, int currentPage, int limit, int currentSize) {
        if (response != null && response.meta() != null && response.meta().pagination() != null) {
            return response.meta().pagination().lastPage() > currentPage;
        }
        return currentSize >= limit;
    }

    private record GeneratedQr(String checkoutUrl, String qrContent, String note) {
    }

    private record VietQrGenerateRequest(
            String accountNo,
            String accountName,
            Integer acqId,
            BigDecimal amount,
            String addInfo,
            String template,
            String format
    ) {
    }

    private record VietQrGenerateResponse(String code, String desc, VietQrGenerateData data) {
    }

    private record VietQrGenerateData(String qrCode, String qrDataURL, String qrLink) {
    }

    private record SePayTransactionsResponse(
            Object status,
            Object error,
            SePayMessages messages,
            List<SePayTransactionItem> transactions,
            List<SePayTransactionItem> data,
            SePayMeta meta
    ) {
    }

    private record SePayMessages(Boolean success) {
    }

    private record SePayMeta(SePayPagination pagination) {
    }

    private record SePayPagination(
            @JsonProperty("total") Integer total,
            @JsonProperty("per_page") Integer perPage,
            @JsonProperty("current_page") Integer currentPage,
            @JsonProperty("last_page") Integer lastPage
    ) {
    }

    private record SePayTransactionItem(
            String id,
            @JsonProperty("bank_brand_name") String bankBrandName,
            @JsonProperty("account_number") String accountNumber,
            @JsonProperty("transaction_date") String transactionDate,
            @JsonProperty("amount_out") String amountOut,
            @JsonProperty("amount_in") String amountIn,
            String accumulated,
            @JsonProperty("transaction_content") String transactionContent,
            @JsonProperty("reference_number") String referenceNumber,
            String code,
            @JsonProperty("sub_account") String subAccount,
            @JsonProperty("bank_account_id") String bankAccountId,
            String description
    ) {
    }

    private record SePayTransaction(String referenceId, BigDecimal amount) {
    }
}
