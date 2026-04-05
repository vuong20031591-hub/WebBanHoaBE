# Rewards Earn/Redeem E2E QA Checklist

## Scope
- Redeem points when creating order from cart.
- Earn points when order becomes CONFIRMED.
- Rollback rewards when order is CANCELLED:
  - Refund redeemed points.
  - Reverse earned points.
- Rewards history must reflect all order-related transactions.

## Manual Check Links

### Frontend
- Checkout: http://localhost:3000/checkout
- Order tracking page: http://localhost:3000/checkout/tracking
- Profile settings: http://localhost:3000/profile/settings

### Backend APIs
- Rewards summary (protected): GET http://localhost:8080/api/rewards
- Rewards history (protected): GET http://localhost:8080/api/rewards/history?page=0&size=50
- Create order from cart (protected): POST http://localhost:8080/api/orders/from-cart
- Confirm COD order (protected): POST http://localhost:8080/api/orders/{id}/cod/confirm
- Admin update order status to CANCELLED: PUT http://localhost:8080/api/admin/orders/{id}/status

## Required Headers
- User endpoints:
  - Authorization: Bearer <USER_ACCESS_TOKEN>
- Admin status update endpoint:
  - X-Role: ADMIN

## Test Scenario A: Redeem + Earn
1. Ensure user has points from GET /api/rewards.
2. Add at least one product to cart.
3. Create order from cart with redeemPoints > 0.
4. Verify order response contains:
   - redeemedPoints > 0
   - rewardsDiscountAmount > 0
5. Confirm COD order.
6. Verify rewards points changed by:
   - -redeemedPoints at order creation.
   - +earnedPoints at confirmation.
7. Verify rewards history has:
   - REDEEM_ORDER transaction with negative points.
   - EARN_ORDER transaction with positive points.

## Test Scenario B: Cancellation Rollback
1. Create and confirm an order with redeemPoints > 0.
2. Cancel order via admin endpoint to status CANCELLED.
3. Verify rewards points are rolled back:
   - +redeemedPoints (refund)
   - -earnedPoints (reverse)
4. Verify rewards history has:
   - REFUND_REDEEM_ORDER transaction.
   - REVERSE_EARN_ORDER transaction.

## Expected Behavior / Validation
- No duplicate reward records for the same order and transaction type.
- No earn points for non-CONFIRMED orders.
- Cancelled order should not keep net rewards impact.
- If earned points were already spent, cancellation should fail with validation error to preserve consistency.
