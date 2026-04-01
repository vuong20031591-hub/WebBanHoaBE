-- Enable RLS on all tables
ALTER TABLE carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;

-- Carts: Users can only access their own cart
CREATE POLICY "Users can view their own cart"
    ON carts FOR SELECT
    USING (auth.uid()::text = user_id);

CREATE POLICY "Users can insert their own cart"
    ON carts FOR INSERT
    WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can update their own cart"
    ON carts FOR UPDATE
    USING (auth.uid()::text = user_id);

CREATE POLICY "Users can delete their own cart"
    ON carts FOR DELETE
    USING (auth.uid()::text = user_id);

-- Cart Items: Users can only access items in their own cart
CREATE POLICY "Users can view their own cart items"
    ON cart_items FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM carts
            WHERE carts.id = cart_items.cart_id
            AND carts.user_id = auth.uid()::text
        )
    );

CREATE POLICY "Users can insert items into their own cart"
    ON cart_items FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM carts
            WHERE carts.id = cart_items.cart_id
            AND carts.user_id = auth.uid()::text
        )
    );

CREATE POLICY "Users can update their own cart items"
    ON cart_items FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM carts
            WHERE carts.id = cart_items.cart_id
            AND carts.user_id = auth.uid()::text
        )
    );

CREATE POLICY "Users can delete their own cart items"
    ON cart_items FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM carts
            WHERE carts.id = cart_items.cart_id
            AND carts.user_id = auth.uid()::text
        )
    );

-- Orders: Users can only access their own orders
CREATE POLICY "Users can view their own orders"
    ON orders FOR SELECT
    USING (auth.uid()::text = user_id);

CREATE POLICY "Users can create their own orders"
    ON orders FOR INSERT
    WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can update their own orders"
    ON orders FOR UPDATE
    USING (auth.uid()::text = user_id);

-- Payment Transactions: Users can only view transactions for their own orders
CREATE POLICY "Users can view their own payment transactions"
    ON payment_transactions FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM orders
            WHERE orders.id = payment_transactions.order_id
            AND orders.user_id = auth.uid()::text
        )
    );

CREATE POLICY "System can insert payment transactions"
    ON payment_transactions FOR INSERT
    WITH CHECK (true);

CREATE POLICY "System can update payment transactions"
    ON payment_transactions FOR UPDATE
    USING (true);

-- Products and Categories remain public (read-only for users)
-- No RLS needed as they are managed by admin only
