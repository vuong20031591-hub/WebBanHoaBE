package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
