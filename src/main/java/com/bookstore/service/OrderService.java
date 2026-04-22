package com.bookstore.service;

import com.bookstore.dto.OrderItemResponse;
import com.bookstore.dto.OrderResponse;
import com.bookstore.model.Book;
import com.bookstore.model.BookOrder;
import com.bookstore.model.Cart;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for order processing.
 *
 * Demonstrates:
 * - Queue<BookOrder> (LinkedList as Queue) for FIFO order processing
 * - offer() to add to the queue, poll() to remove from the front
 * - ArrayList<OrderItem> for ordered line items in each order
 */
@Service
public class OrderService {

    // Queue: orders are processed in FIFO order (first in, first out)
    // LinkedList implements the Queue interface
    private final Queue<BookOrder> pendingOrders = new LinkedList<>();

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final BookRepository bookRepository;
    private final CartService cartService;

    public OrderService(BookRepository bookRepository, CartService cartService) {
        this.bookRepository = bookRepository;
        this.cartService = cartService;
    }

    /**
     * Creates an order from the current cart contents.
     *
     * 1. Reads the cart items (Map<Long, Integer>)
     * 2. Builds an ArrayList<OrderItem> — one item per book in the cart
     * 3. Creates a BookOrder and adds it to the Queue using offer()
     * 4. Clears the cart
     */
    public OrderResponse checkout() {
        Cart cart = cartService.getCart();
        if (cart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        // Build the list of order items from the cart's Map entries
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cart.getItems().entrySet()) {
            Long bookId = entry.getKey();
            int quantity = entry.getValue();

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId));
            items.add(new OrderItem(bookId, book.getTitle(), quantity, book.getPrice()));
        }

        // Create the order and add it to the queue
        BookOrder order = new BookOrder(
                idGenerator.getAndIncrement(),
                items,
                LocalDateTime.now()
        );

        // offer() adds the order to the end of the queue
        pendingOrders.offer(order);

        // Clear the cart after checkout
        cartService.clearCart();

        return toResponse(order);
    }

    /**
     * Processes the next order in line (FIFO).
     *
     * poll() removes and returns the head of the queue,
     * or returns null if the queue is empty.
     */
    public OrderResponse processNextOrder() {
        // poll() removes the first element from the queue (FIFO)
        BookOrder next = pendingOrders.poll();
        if (next == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending orders");
        }
        next.setStatus(OrderStatus.PROCESSED);
        return toResponse(next);
    }

    /**
     * Returns all pending orders without removing them from the queue.
     */
    public List<OrderResponse> getPendingOrders() {
        List<OrderResponse> result = new ArrayList<>();
        for (BookOrder order : pendingOrders) {
            result.add(toResponse(order));
        }
        return result;
    }

    private OrderResponse toResponse(BookOrder order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemResponses.add(new OrderItemResponse(
                    item.getBookId(),
                    item.getTitle(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()
            ));
        }
        return new OrderResponse(
                order.getId(),
                itemResponses,
                order.getPlacedAt(),
                order.getStatus().name()
        );
    }
}
