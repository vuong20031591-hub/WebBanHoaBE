package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AddCartItemRequest;
import com.florastore.web_ban_hoa.dto.CartResponse;
import com.florastore.web_ban_hoa.dto.UpdateCartItemRequest;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final JwtSubjectResolver jwtSubjectResolver;

    public CartController(CartService cartService, JwtSubjectResolver jwtSubjectResolver) {
        this.cartService = cartService;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(cartService.addItem(userId, request.productId(), request.quantity()));
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("Authorization") String authorization) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, id, request.quantity()));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        cartService.deleteItem(userId, id);
        return ResponseEntity.noContent().build();
    }
}
