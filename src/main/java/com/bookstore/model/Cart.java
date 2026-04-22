package com.bookstore.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple in-memory shopping cart.
 *
 * Demonstrates HashMap usage:
 * - Map<Long, Integer> maps book IDs to quantities
 * - merge() combines values when adding the same book twice
 * - Collections.unmodifiableMap() returns a read-only view
 */
public class Cart {

    // HashMap: bookId -> quantity
    private final Map<Long, Integer> items = new HashMap<>();

    /**
     * Adds a book to the cart.
     * If the book is already in the cart, merge() adds the quantities together.
     *
     * merge(key, value, remappingFunction):
     *   - If key is absent: puts the value
     *   - If key is present: applies the function to combine old and new values
     *   - Integer::sum adds old quantity + new quantity
     */
    public void addItem(Long bookId, int quantity) {
        items.merge(bookId, quantity, Integer::sum);
    }

    /**
     * Removes a book entirely from the cart.
     */
    public void removeItem(Long bookId) {
        items.remove(bookId);
    }

    /**
     * Returns a read-only view of the cart contents.
     * Collections.unmodifiableMap() prevents callers from modifying the internal map.
     */
    public Map<Long, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Removes all items from the cart.
     */
    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
