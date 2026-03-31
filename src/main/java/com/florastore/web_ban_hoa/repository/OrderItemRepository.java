package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
