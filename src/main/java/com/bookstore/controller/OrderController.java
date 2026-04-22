package com.bookstore.controller;

import com.bookstore.dto.OrderResponse;
import com.bookstore.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders/checkout — creates an order from the current cart.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout() {
        OrderResponse order = orderService.checkout();
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * POST /api/orders/process-next — processes the next order in the queue (FIFO).
     */
    @PostMapping("/process-next")
    public ResponseEntity<OrderResponse> processNext() {
        return ResponseEntity.ok(orderService.processNextOrder());
    }

    /**
     * GET /api/orders/pending — returns all orders waiting to be processed.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        return ResponseEntity.ok(orderService.getPendingOrders());
    }
}
