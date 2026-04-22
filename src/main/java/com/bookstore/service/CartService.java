package com.bookstore.service;

import com.bookstore.dto.CartItemResponse;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for shopping cart operations.
 *
 * Demonstrates:
 * - Map<String, Cart> mapping usernames to their carts
 * - computeIfAbsent() to lazily create a cart on first access
 * - HashMap via the Cart model (bookId -> quantity)
 * - Map.entrySet() to iterate over key-value pairs
 * - Building a List<CartItemResponse> from Map entries
 */
@Service
public class CartService {

    // Each user gets their own cart, keyed by username
    // computeIfAbse    nt() creates a new Cart the first time a user accesses it
    private final Map<String, Cart> carts = new HashMap<>();
    private final BookRepository bookRepository;

    public CartService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Returns the cart for the currently logged-in user.
     *
     * computeIfAbsent(key, mappingFunction):
     *   - If the key exists: returns the existing value
     *   - If the key is absent: calls the function to create a value, stores it, and returns it
     */
    private Cart getCartForCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return carts.computeIfAbsent(username, key -> new Cart());
    }

    public void addItem(Long bookId, int quantity) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId);
        }
        getCartForCurrentUser().addItem(bookId, quantity);
    }

    public void removeItem(Long bookId) {
        getCartForCurrentUser().removeItem(bookId);
    }

    /**
     * Returns all cart items with book details and computed subtotals.
     *
     * Iterates over the cart's Map using entrySet().
     * Each entry has a key (bookId) and a value (quantity).
     */
    public List<CartItemResponse> getCartItems() {
        List<CartItemResponse> result = new ArrayList<>();

        // entrySet() returns all key-value pairs from the Map
        for (Map.Entry<Long, Integer> entry : getCartForCurrentUser().getItems().entrySet()) {
            Long bookId = entry.getKey();
            int quantity = entry.getValue();

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId));
            BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(quantity));

            result.add(new CartItemResponse(
                    bookId,
                    book.getTitle(),
                    quantity,
                    book.getPrice(),
                    subtotal
            ));
        }

        return result;
    }

    public void clearCart() {
        getCartForCurrentUser().clear();
    }

    public Cart getCart() {
        return getCartForCurrentUser();
    }
}
