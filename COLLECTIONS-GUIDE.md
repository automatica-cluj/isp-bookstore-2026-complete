# Java Collections in the Bookstore API -- A Practical Guide

## Overview

This guide walks you through every Java collection type used in the `06-bookstore-api-collections` project. Instead of learning collections from abstract examples, you will see them solving real problems inside a Spring Boot application -- storing unique tags, managing a shopping cart, grouping books by author, and processing orders in a queue.

!!! abstract "Learning Objectives"
    By the end of this guide, you will be able to:

    - [ ] Explain when to use `Set`, `List`, `Map`, and `Queue`
    - [ ] Identify the concrete implementation class (`HashSet`, `ArrayList`, `HashMap`, `LinkedList`) appropriate for each use case
    - [ ] Read and understand `Map.entrySet()` iteration and `Collectors.groupingBy()`
    - [ ] Use `Collections.unmodifiableMap()` to create read-only views
    - [ ] Trace how collections flow from model classes through services to REST endpoints

## Prerequisites

- Basic Java syntax (variables, loops, methods, classes)
- Understanding of generics (`List<String>`, `Map<Long, Integer>`)
- Familiarity with the Java Collections Framework interfaces (`List`, `Set`, `Map`, `Queue`)

---

## 1. HashSet -- Unique Tags per Book

### The Problem

Each book can have tags like "programming", "bestseller", or "java". A book should never have the **same tag twice**. We need a collection that automatically prevents duplicates.

### Why HashSet?

A **`Set`** is the right choice when you care about **uniqueness** and don't care about order. `HashSet` is the most common `Set` implementation -- it uses hashing for O(1) lookups and automatically rejects duplicates.

### Where It's Used

**File:** `model/Book.java`

```java
// Set<String> -- stores unique tags for each book (e.g., "programming", "bestseller")
// @ElementCollection tells JPA to store these in a separate table (book_tags)
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "book_tags", joinColumns = @JoinColumn(name = "book_id"))
@Column(name = "tag")
private Set<String> tags = new HashSet<>();
```

Notice the constructor makes a **defensive copy** -- it creates a new `HashSet` from the input to prevent the caller from modifying the book's internal set later:

```java
public Book(String title, Author author, String isbn, BigDecimal price, Set<String> tags) {
    this.title = title;
    this.author = author;
    this.isbn = isbn;
    this.price = price;
    this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
}
```

**File:** `service/BookService.java` -- The `getAllTags()` method collects every unique tag across **all** books:

```java
public Set<String> getAllTags() {
    List<Book> allBooks = bookRepository.findAll();

    Set<String> allTags = new HashSet<>();   // duplicates are automatically ignored
    for (Book book : allBooks) {
        allTags.addAll(book.getTags());      // addAll() adds each tag, skipping duplicates
    }
    return allTags;
}
```

If Book A has tags `["java", "programming"]` and Book B has tags `["java", "bestseller"]`, the result is `["java", "programming", "bestseller"]` -- `"java"` appears only once.

**File:** `controller/BookController.java` -- The endpoint that exposes this:

```java
@GetMapping("/tags")
public ResponseEntity<Set<String>> getAllTags() {
    return ResponseEntity.ok(bookService.getAllTags());
}
```

!!! tip "When to use Set"
    Use a `Set` whenever your data represents a collection of **unique** items and you don't need to access elements by index. Common examples: tags, categories, roles, permissions.

---

## 2. HashMap -- Shopping Cart (Book ID to Quantity)

### The Problem

A shopping cart needs to map each book to a quantity. If a customer adds the same book twice, the quantities should be **combined**, not duplicated. We also want to prevent external code from accidentally modifying the cart's internal state.

### Why HashMap?

A **`Map`** stores key-value pairs. `HashMap` gives us O(1) lookup by key, which is perfect for looking up "how many copies of book #5 are in the cart?"

### Where It's Used

**File:** `model/Cart.java`

```java
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
```

### Key Methods Demonstrated

| Method | What It Does | Example |
|--------|-------------|---------|
| `merge(key, value, function)` | Adds the value if key is new; combines old + new values if key exists | `merge(5L, 2, Integer::sum)` -- adds 2 copies of book #5, or increases existing quantity by 2 |
| `Collections.unmodifiableMap(items)` | Returns a view that throws an exception if someone tries to `put()` or `remove()` | Protects the cart's internal state from accidental modification |
| `remove(key)` | Deletes the entry for that key | `remove(5L)` -- removes book #5 from the cart entirely |
| `clear()` | Removes all entries | Used after checkout to empty the cart |

!!! warning "Why unmodifiableMap matters"
    Without `Collections.unmodifiableMap()`, any code that calls `cart.getItems()` could add or remove items directly, bypassing the `addItem()` and `removeItem()` methods. This would break encapsulation -- a core OOP principle.

---

## 3. Map.entrySet() -- Iterating Over Key-Value Pairs

### The Problem

Once we have a `Map<Long, Integer>` (book ID to quantity), we need to iterate over it to build response objects or create order line items. But a `Map` is not iterable by itself -- you need to choose **how** to iterate: by keys, by values, or by key-value pairs.

### Why entrySet()?

`entrySet()` gives you both the key and the value in each iteration step. This is exactly what we need when we want to look up a book (by key) **and** use its quantity (the value).

### Where It's Used

**File:** `service/CartService.java` -- Building cart item responses from the Map:

```java
public List<CartItemResponse> getCartItems() {
    List<CartItemResponse> result = new ArrayList<>();

    // entrySet() returns all key-value pairs from the Map
    for (Map.Entry<Long, Integer> entry : cart.getItems().entrySet()) {
        Long bookId = entry.getKey();       // the key: book ID
        int quantity = entry.getValue();     // the value: quantity

        Book book = bookRepository.findById(bookId).orElseThrow();
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
```

**File:** `service/OrderService.java` -- Building order line items during checkout:

```java
// Build the list of order items from the cart's Map entries
List<OrderItem> items = new ArrayList<>();
for (Map.Entry<Long, Integer> entry : cart.getItems().entrySet()) {
    Long bookId = entry.getKey();
    int quantity = entry.getValue();

    Book book = bookRepository.findById(bookId).orElseThrow();
    items.add(new OrderItem(bookId, book.getTitle(), quantity, book.getPrice()));
}
```

!!! note "Three ways to iterate a Map"
    - `map.keySet()` -- iterate over keys only
    - `map.values()` -- iterate over values only
    - `map.entrySet()` -- iterate over key-value pairs (most common when you need both)

---

## 4. Collectors.groupingBy() -- Grouping Books by Author

### The Problem

We want an endpoint that returns all books organized by author name. The result should look like:

```json
{
  "Joshua Bloch": [ { "title": "Effective Java", ... } ],
  "Robert Martin": [ { "title": "Clean Code", ... }, { "title": "Clean Architecture", ... } ]
}
```

### Why groupingBy()?

`Collectors.groupingBy()` is a **stream collector** that builds a `Map<K, List<V>>` in one pass. You provide a function that extracts the grouping key, and it does the rest. This is much cleaner than manually building the Map with loops.

### Where It's Used

**File:** `service/BookService.java`

```java
public Map<String, List<BookResponse>> getBooksByAuthor() {
    List<Book> allBooks = bookRepository.findAll();

    // Group books by author name using Collectors.groupingBy()
    return allBooks.stream()
            .collect(Collectors.groupingBy(
                    // Key: the author's full name
                    book -> book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName(),
                    // Value: each book is mapped to a BookResponse before being added to the list
                    Collectors.mapping(this::toResponse, Collectors.toList())
            ));
}
```

**File:** `controller/BookController.java`

```java
@GetMapping("/by-author")
public ResponseEntity<Map<String, List<BookResponse>>> getBooksByAuthor() {
    return ResponseEntity.ok(bookService.getBooksByAuthor());
}
```

!!! tip "How groupingBy works step by step"
    1. The stream iterates over each `Book`
    2. For each book, the first argument extracts the key (author name)
    3. The second argument (`Collectors.mapping(...)`) transforms each book into a `BookResponse`
    4. Books with the same key are collected into the same `List`
    5. The result is a `Map<String, List<BookResponse>>`

---

## 5. ArrayList -- Ordered Line Items

### The Problem

An order contains line items (which books were purchased and in what quantity). The order of items matters (for display purposes), and we need to build the list incrementally using `add()`.

### Why ArrayList?

**`ArrayList`** is the go-to `List` implementation. It maintains insertion order, provides fast index-based access (O(1)), and is efficient for the `add()` operations we use here.

### Where It's Used

**File:** `model/BookOrder.java` -- Each order holds a list of line items:

```java
public class BookOrder {

    private Long id;
    private List<OrderItem> items;  // ArrayList -- ordered line items
    private LocalDateTime placedAt;
    private OrderStatus status;

    public BookOrder(Long id, List<OrderItem> items, LocalDateTime placedAt) {
        this.id = id;
        this.items = items;
        this.placedAt = placedAt;
        this.status = OrderStatus.PENDING;
    }
    // ...
}
```

**File:** `service/OrderService.java` -- Building the list during checkout:

```java
List<OrderItem> items = new ArrayList<>();
for (Map.Entry<Long, Integer> entry : cart.getItems().entrySet()) {
    Long bookId = entry.getKey();
    int quantity = entry.getValue();

    Book book = bookRepository.findById(bookId).orElseThrow();
    items.add(new OrderItem(bookId, book.getTitle(), quantity, book.getPrice()));
}
```

**File:** `model/Author.java` -- JPA `@OneToMany` relationship:

```java
@OneToMany(mappedBy = "author")
private List<Book> books = new ArrayList<>();
```

Here, `ArrayList` is used because JPA needs a mutable `List` to manage the one-to-many relationship between an author and their books.

!!! note "List vs Set for JPA relationships"
    Use `List` when the order of elements matters or when you need index-based access. Use `Set` when you need uniqueness guarantees (like tags). In JPA, `List` is the more common choice for `@OneToMany` relationships.

---

## 6. LinkedList as Queue -- FIFO Order Processing

### The Problem

When customers check out, their orders should be processed in the order they were placed -- **first in, first out** (FIFO). We need a collection that efficiently supports adding to the back and removing from the front.

### Why LinkedList (as Queue)?

The **`Queue`** interface defines FIFO behavior with `offer()` (add to back) and `poll()` (remove from front). `LinkedList` implements `Queue` and provides O(1) performance for both operations.

### Where It's Used

**File:** `service/OrderService.java`

```java
// Queue: orders are processed in FIFO order (first in, first out)
// LinkedList implements the Queue interface
private final Queue<BookOrder> pendingOrders = new LinkedList<>();
```

**Adding to the queue** with `offer()`:

```java
// offer() adds the order to the end of the queue
pendingOrders.offer(order);
```

**Removing from the queue** with `poll()`:

```java
public OrderResponse processNextOrder() {
    // poll() removes the first element from the queue (FIFO)
    BookOrder next = pendingOrders.poll();
    if (next == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending orders");
    }
    next.setStatus(OrderStatus.PROCESSED);
    return toResponse(next);
}
```

**Iterating without removing** (to list all pending orders):

```java
public List<OrderResponse> getPendingOrders() {
    List<OrderResponse> result = new ArrayList<>();
    for (BookOrder order : pendingOrders) {   // iterates in FIFO order, does NOT remove
        result.add(toResponse(order));
    }
    return result;
}
```

### Key Queue Methods

| Method | What It Does | Returns if Empty |
|--------|-------------|------------------|
| `offer(element)` | Adds to the **end** of the queue | `false` (but effectively always succeeds for `LinkedList`) |
| `poll()` | Removes and returns the **first** element | `null` |
| `peek()` | Returns the first element **without** removing it | `null` |

!!! warning "offer/poll vs add/remove"
    `Queue` has two sets of methods: `offer()`/`poll()`/`peek()` return special values (`null` or `false`) on failure, while `add()`/`remove()`/`element()` throw exceptions. In most cases, prefer `offer()`/`poll()`/`peek()` -- they are safer.

---

## Summary Table

| Collection Type | Class | Where Used | Why This Collection? |
|----------------|-------|-----------|---------------------|
| `Set<String>` | `HashSet` | `Book.java` -- tags per book | Prevents duplicate tags |
| `Set<String>` | `HashSet` | `BookService.getAllTags()` | Collects unique tags across all books |
| `Map<Long, Integer>` | `HashMap` | `Cart.java` -- bookId to quantity | Fast lookup by book ID; `merge()` combines quantities |
| `Map.Entry` iteration | `HashMap.entrySet()` | `CartService.getCartItems()` | Iterates over cart entries to build response objects |
| `Map.Entry` iteration | `HashMap.entrySet()` | `OrderService.checkout()` | Iterates over cart entries to build order line items |
| `Map<String, List<>>` | `groupingBy()` | `BookService.getBooksByAuthor()` | Groups books by author name in one stream pass |
| `List<OrderItem>` | `ArrayList` | `BookOrder.java` -- order line items | Ordered items, fast index access, incremental building |
| `List<Book>` | `ArrayList` | `Author.java` -- JPA OneToMany | JPA needs a mutable List for relationship mapping |
| `Queue<BookOrder>` | `LinkedList` | `OrderService` -- pending orders | FIFO processing: `offer()` to enqueue, `poll()` to dequeue |

---

## API Endpoints by Collection Type

| Endpoint | HTTP Method | Collection Demonstrated |
|----------|-------------|------------------------|
| `/api/books` | GET | `Set<String>` tags on each `BookResponse` |
| `/api/books/tags` | GET | `HashSet` collecting unique tags across all books |
| `/api/books/by-author` | GET | `Collectors.groupingBy()` returning `Map<String, List<BookResponse>>` |
| `/api/cart/items` | POST | `HashMap.merge()` adding items to the cart |
| `/api/cart` | GET | `Map.entrySet()` iteration building response list |
| `/api/orders/checkout` | POST | `ArrayList.add()` building line items + `Queue.offer()` enqueuing order |
| `/api/orders/pending` | GET | `Queue` iteration (without removal) |
| `/api/orders/process-next` | POST | `Queue.poll()` dequeuing the first order |

---

## Try It Yourself

Use these `curl` commands to see every collection type in action. Run them **in order** -- each step builds on the previous one.

!!! info "Prerequisites"
    Make sure the application is running on `http://localhost:8080`. If authentication is enabled, you may need to register and log in first, then include the JWT token as a header: `-H "Authorization: Bearer <token>"`.

### Step 1: See Tags as a Set

Each book has a `tags` field that is a `Set<String>` -- no duplicates allowed.

```bash
curl -s http://localhost:8080/api/books | jq '.[0].tags'
```

### Step 2: Get All Unique Tags

The `HashSet` in `BookService.getAllTags()` collects tags from every book and removes duplicates automatically.

```bash
curl -s http://localhost:8080/api/books/tags | jq .
```

### Step 3: Group Books by Author

`Collectors.groupingBy()` builds a `Map<String, List<BookResponse>>` in one pass.

```bash
curl -s http://localhost:8080/api/books/by-author | jq .
```

### Step 4: Add Items to the Cart (HashMap)

Each call uses `HashMap.merge()` under the hood. Adding the same book twice increases the quantity.

```bash
# Add 2 copies of book #1
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1, "quantity": 2}'

# Add 1 copy of book #2
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"bookId": 2, "quantity": 1}'

# Add 1 more copy of book #1 (merge adds quantities: 2 + 1 = 3)
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1, "quantity": 1}'
```

### Step 5: View the Cart (Map.entrySet Iteration)

The service iterates over `cart.getItems().entrySet()` to build the response. Notice book #1 now has quantity 3.

```bash
curl -s http://localhost:8080/api/cart | jq .
```

### Step 6: Checkout (ArrayList + Queue.offer)

Checkout builds an `ArrayList<OrderItem>` from the cart entries, creates a `BookOrder`, and adds it to the `Queue` with `offer()`.

```bash
curl -s -X POST http://localhost:8080/api/orders/checkout | jq .
```

### Step 7: View Pending Orders (Queue Iteration)

Iterates over the `Queue<BookOrder>` without removing any elements.

```bash
curl -s http://localhost:8080/api/orders/pending | jq .
```

### Step 8: Process the Next Order (Queue.poll)

`poll()` removes and returns the first order in the queue (FIFO).

```bash
curl -s -X POST http://localhost:8080/api/orders/process-next | jq .
```

After processing, check that the pending queue is now empty:

```bash
curl -s http://localhost:8080/api/orders/pending | jq .
```

---

## Quick Reference: Choosing the Right Collection

When you are deciding which collection to use, ask yourself these questions:

```
Do you need key-value pairs?
  YES --> Use a Map (HashMap for general use)
  NO  --> Do you need uniqueness?
            YES --> Use a Set (HashSet for general use)
            NO  --> Do you need FIFO ordering?
                      YES --> Use a Queue (LinkedList)
                      NO  --> Use a List (ArrayList for general use)
```

## Further Reading

- [Java Collections Framework Overview](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/doc-files/coll-overview.html) -- Official Oracle documentation
- [Baeldung: Guide to Java Collections](https://www.baeldung.com/java-collections) -- Practical tutorials with examples
- [Collectors.groupingBy() Javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Collectors.html#groupingBy(java.util.function.Function)) -- Official reference for the groupingBy collector
