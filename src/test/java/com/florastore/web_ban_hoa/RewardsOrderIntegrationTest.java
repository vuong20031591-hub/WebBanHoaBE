package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.dto.CreateOrderFromCartRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.CartService;
import com.florastore.web_ban_hoa.service.OrderService;
import com.florastore.web_ban_hoa.service.RewardsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class RewardsOrderIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RewardsService rewardsService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void orderFromCart_shouldApplyRedeemAndAwardPointsWhenConfirmed() {
        String userId = String.valueOf(System.currentTimeMillis());

        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        cartService.addItem(userId, product.getId(), 1);

        int initialPoints = rewardsService.getUserRewards(userId).points();
        int redeemPoints = Math.min(100, initialPoints);

        OrderResponse createdOrder = orderService.createOrderFromCart(
                userId,
                new CreateOrderFromCartRequest(PaymentMethod.COD, null, redeemPoints)
        );

        BigDecimal expectedDiscount = RewardsService.REDEEM_POINT_VALUE_VND.multiply(BigDecimal.valueOf(redeemPoints));
        BigDecimal expectedPaidAmount = product.getPrice().subtract(expectedDiscount).max(BigDecimal.ZERO);

        assertThat(createdOrder.redeemedPoints()).isEqualTo(redeemPoints);
        assertThat(createdOrder.rewardsDiscountAmount()).isEqualByComparingTo(expectedDiscount);
        assertThat(createdOrder.totalAmount()).isEqualByComparingTo(expectedPaidAmount);

        OrderResponse confirmedOrder = orderService.confirmCodOrder(userId, createdOrder.id());
        assertThat(confirmedOrder.status()).isEqualTo("CONFIRMED");

        int earnedPoints = expectedPaidAmount
                .divide(RewardsService.EARN_POINT_STEP_VND, 0, RoundingMode.DOWN)
                .intValue();

        int expectedFinalPoints = initialPoints - redeemPoints + earnedPoints;
        int actualFinalPoints = rewardsService.getUserRewards(userId).points();
        assertThat(actualFinalPoints).isEqualTo(expectedFinalPoints);

        var history = rewardsService.getTransactionHistory(userId, 0, 20).getContent();
        assertThat(history).anyMatch(tx ->
                RewardsService.REWARD_TYPE_ORDER_REDEEM.equals(tx.type())
                        && tx.orderId().equals(createdOrder.id())
                        && tx.points().equals(-redeemPoints)
        );
        assertThat(history).anyMatch(tx ->
                RewardsService.REWARD_TYPE_ORDER_EARN.equals(tx.type())
                        && tx.orderId().equals(createdOrder.id())
                        && tx.points().equals(earnedPoints)
        );
    }

    @Test
    void cancellingConfirmedOrder_shouldRollbackRedeemAndEarnTransactions() {
        String userId = String.valueOf(System.currentTimeMillis() + 1000);

        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        cartService.addItem(userId, product.getId(), 1);

        int initialPoints = rewardsService.getUserRewards(userId).points();
        int redeemPoints = Math.min(100, initialPoints);

        OrderResponse createdOrder = orderService.createOrderFromCart(
                userId,
                new CreateOrderFromCartRequest(PaymentMethod.COD, null, redeemPoints)
        );

        OrderResponse confirmedOrder = orderService.confirmCodOrder(userId, createdOrder.id());
        assertThat(confirmedOrder.status()).isEqualTo("CONFIRMED");

        OrderResponse cancelledOrder = orderService.updateOrderStatus(createdOrder.id(), OrderStatus.CANCELLED);
        assertThat(cancelledOrder.status()).isEqualTo("CANCELLED");

        int finalPoints = rewardsService.getUserRewards(userId).points();
        assertThat(finalPoints).isEqualTo(initialPoints);

        int earnedPoints = confirmedOrder.totalAmount()
                .divide(RewardsService.EARN_POINT_STEP_VND, 0, RoundingMode.DOWN)
                .intValue();

        var history = rewardsService.getTransactionHistory(userId, 0, 50).getContent();
        assertThat(history).anyMatch(tx ->
                RewardsService.REWARD_TYPE_ORDER_REDEEM.equals(tx.type())
                        && tx.orderId().equals(createdOrder.id())
                        && tx.points().equals(-redeemPoints)
        );
        assertThat(history).anyMatch(tx ->
                RewardsService.REWARD_TYPE_ORDER_EARN.equals(tx.type())
                        && tx.orderId().equals(createdOrder.id())
                        && tx.points().equals(earnedPoints)
        );
        assertThat(history).anyMatch(tx ->
                RewardsService.REWARD_TYPE_ORDER_REDEEM_REFUND.equals(tx.type())
                        && tx.orderId().equals(createdOrder.id())
                        && tx.points().equals(redeemPoints)
        );
        assertThat(history).anyMatch(tx ->
                RewardsService.REWARD_TYPE_ORDER_EARN_REVERSE.equals(tx.type())
                        && tx.orderId().equals(createdOrder.id())
                        && tx.points().equals(-earnedPoints)
        );
    }
}
