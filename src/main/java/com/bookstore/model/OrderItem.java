package com.bookstore.model;

import java.math.BigDecimal;

/**
 * Represents one line item in an order.
 * Stored in an ArrayList<OrderItem> inside each Order.
 */
public class OrderItem {

    private Long bookId;
    private String title;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem(Long bookId, String title, int quantity, BigDecimal unitPrice) {
        this.bookId = bookId;
        this.title = title;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
