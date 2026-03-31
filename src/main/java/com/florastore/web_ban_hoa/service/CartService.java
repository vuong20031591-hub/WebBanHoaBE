package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.CartResponse;

public interface CartService {
    CartResponse addItem(String userId, Long productId, Integer quantity);

    CartResponse getCart(String userId);

    CartResponse updateItemQuantity(String userId, Long cartItemId, Integer quantity);

    void deleteItem(String userId, Long cartItemId);
}
