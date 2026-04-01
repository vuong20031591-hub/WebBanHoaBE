package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
