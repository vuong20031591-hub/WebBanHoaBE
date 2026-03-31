package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.CartItemResponse;
import com.florastore.web_ban_hoa.dto.CartResponse;
import com.florastore.web_ban_hoa.entity.Cart;
import com.florastore.web_ban_hoa.entity.CartItem;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.CartItemRepository;
import com.florastore.web_ban_hoa.repository.CartRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    public CartResponse addItem(String userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        Product product = getProductOrThrow(productId);
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        CartItem cartItem = null;

        if (cart != null) {
            cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
        }

        if (cartItem == null) {
            validateStock(product, quantity);

            if (cart == null) {
                cart = cartRepository.save(new Cart(userId));
            }

            cartItem = new CartItem(cart, product, quantity, product.getPrice());
            cart.getItems().add(cartItem);
        } else {
            int newQuantity = cartItem.getQuantity() + quantity;
            validateStock(product, newQuantity);
            cartItem.setQuantity(newQuantity);
            cartItem.setPrice(product.getPrice());
        }

        cartRepository.save(cart);
        return toCartResponse(loadCartOrThrow(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(new Cart(userId));
        return toCartResponse(cart);
    }

    @Override
    public CartResponse updateItemQuantity(String userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        Cart cart = loadCartOrThrow(userId);
        CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        Product product = getProductOrThrow(cartItem.getProduct().getId());
        validateStock(product, quantity);
        cartItem.setQuantity(quantity);
        cartItem.setPrice(product.getPrice());

        cartItemRepository.save(cartItem);
        return toCartResponse(loadCartOrThrow(userId));
    }

    @Override
    public void deleteItem(String userId, Long cartItemId) {
        Cart cart = loadCartOrThrow(userId);
        CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        cart.getItems().removeIf(item -> item.getId().equals(cartItem.getId()));
        cartRepository.save(cart);
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Cart loadCartOrThrow(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
    }

    private void validateStock(Product product, Integer requiredQuantity) {
        Integer stock = product.getStockQuantity();
        if (stock == null || requiredQuantity > stock) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock for product id " + product.getId()
            );
        }
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = new ArrayList<>();
        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            BigDecimal currentPrice = product.getPrice();
            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            items.add(new CartItemResponse(
                    item.getId(),
                    product.getId(),
                    product.getName(),
                    product.getImage(),
                    item.getQuantity(),
                    currentPrice,
                    lineTotal,
                    product.getStockQuantity()
            ));

            totalItems += item.getQuantity();
            totalAmount = totalAmount.add(lineTotal);
        }

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                totalItems,
                totalAmount,
                items,
                cart.getUpdatedAt()
        );
    }
}
