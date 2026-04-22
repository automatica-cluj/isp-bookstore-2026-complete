package com.bookstore.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a customer order (in-memory, not persisted to DB).
 *
 * Demonstrates:
 * - ArrayList<OrderItem> for ordered line items
 * - Each order stores its items as a List — order matters, index access is fast
 */
public class BookOrder {

    private Long id;
    private List<OrderItem> items;  // ArrayList — ordered line items
    private LocalDateTime placedAt;
    private OrderStatus status;

    public BookOrder(Long id, List<OrderItem> items, LocalDateTime placedAt) {
        this.id = id;
        this.items = items;
        this.placedAt = placedAt;
        this.status = OrderStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
