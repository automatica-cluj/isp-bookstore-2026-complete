# Bookstore API — Developer Guide

High-level architecture and implementation reference for **06-bookstore-api-collections**.

---

## 1. Application Overview

A full-stack bookstore application built with **Spring Boot 3.4.3** (Java 17) and a **vanilla JavaScript SPA** frontend. The primary teaching goal is demonstrating the **Java Collections Framework** (Set, Map, Queue, List) in a realistic web application context, layered on top of JWT-secured REST APIs.

### Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.3 (Web, Security, Data JPA, Validation) |
| Authentication | JWT (JJWT 0.12.6) with BCrypt password encoding |
| Database (dev) | H2 in-memory |
| Database (prod) | PostgreSQL 16 via Docker Compose |
| ORM | Hibernate / Spring Data JPA |
| Frontend | Vanilla HTML/CSS/JavaScript (no build tools) |
| Containerization | Docker multi-stage build + Compose |

---

## 2. Project Structure

```
src/main/
├── java/com/bookstore/
│   ├── BookstoreApplication.java        # Spring Boot entry point
│   ├── config/
│   │   └── WebConfig.java               # CORS configuration
│   ├── controller/
│   │   ├── AuthController.java          # POST /api/auth/login, /register
│   │   ├── AuthorController.java        # CRUD /api/authors
│   │   ├── BookController.java          # CRUD /api/books + tags/by-author
│   │   ├── CartController.java          # /api/cart operations
│   │   ├── OrderController.java         # /api/orders checkout + processing
│   │   └── GlobalExceptionHandler.java  # Centralized error responses
│   ├── dto/                             # Request/response records (11 files)
│   ├── model/
│   │   ├── Author.java                  # JPA entity
│   │   ├── Book.java                    # JPA entity (has @ElementCollection tags)
│   │   ├── User.java                    # JPA entity
│   │   ├── Cart.java                    # In-memory POJO (HashMap-based)
│   │   ├── BookOrder.java               # In-memory POJO (ArrayList items)
│   │   ├── OrderItem.java               # In-memory POJO
│   │   ├── OrderStatus.java             # Enum: PENDING, PROCESSED
│   │   └── Role.java                    # Enum: USER, ADMIN
│   ├── repository/                      # JPA repositories (3 interfaces)
│   ├── security/
│   │   ├── SecurityConfig.java          # Route authorization + filter chain
│   │   ├── JwtUtil.java                 # Token generate/validate
│   │   ├── JwtAuthFilter.java           # Per-request token extraction
│   │   └── CustomUserDetailsService.java
│   └── service/
│       ├── AuthorService.java
│       ├── BookService.java             # Tags (Set) + grouping (Map)
│       ├── CartService.java             # Per-user carts (Map<String,Cart>)
│       └── OrderService.java            # Order queue (LinkedList as Queue)
└── resources/
    ├── application.yml                  # Dev config (H2)
    ├── application-docker.yml           # Prod config (PostgreSQL)
    ├── data.sql                         # Seed data (3 authors, 3 books)
    └── static/                          # Frontend SPA
        ├── index.html
        ├── css/style.css
        └── js/  (api, app, auth, authors, books, cart, orders)
```

---

## 3. Backend Architecture

### 3.1 Layered Design

```
┌─────────────────────────────────────────────┐
│                  HTTP Request                │
├─────────────────────────────────────────────┤
│  JwtAuthFilter  (extract + validate token)  │
├─────────────────────────────────────────────┤
│  SecurityConfig (route authorization rules) │
├─────────────────────────────────────────────┤
│  Controller Layer  (REST endpoints + DTOs)  │
├─────────────────────────────────────────────┤
│  Service Layer  (business logic)            │
├─────────────────────────────────────────────┤
│  Repository Layer  (Spring Data JPA)        │
├─────────────────────────────────────────────┤
│  Database  (H2 dev / PostgreSQL prod)       │
└─────────────────────────────────────────────┘
```

Every request passes through the JWT filter before reaching controllers. The service layer contains all business logic and collections usage. DTOs (Java records) decouple the API contract from internal entities.

### 3.2 Domain Model

**Persisted (JPA entities)**:

| Entity | Table | Key Fields | Relationships |
|--------|-------|------------|---------------|
| `User` | `users` | username (unique), password (BCrypt), role (USER/ADMIN) | — |
| `Author` | `authors` | firstName, lastName | One-to-Many → Book |
| `Book` | `books` | title, isbn (unique), price, tags (Set&lt;String&gt;) | Many-to-One → Author |

The `Book.tags` field uses `@ElementCollection` — Hibernate stores tags in a separate `book_tags` join table, loaded as a `Set<String>`.

**In-memory (not persisted)**:

| Model | Backing Structure | Purpose |
|-------|-------------------|---------|
| `Cart` | `HashMap<Long, Integer>` | bookId → quantity mapping per user |
| `BookOrder` | `ArrayList<OrderItem>` | Ordered list of line items |
| `OrderItem` | POJO | bookId, title, quantity, unitPrice + computed subtotal |

Cart and order data lives only in JVM memory — it resets on restart. This is intentional: the goal is demonstrating collections, not building a production order system.

### 3.3 Service Layer — Collections Showcase

Each service demonstrates specific Java Collections patterns:

**BookService**:
- `getAllTags()` — iterates all books, collects tags into a `HashSet<String>` for automatic deduplication
- `getBooksByAuthor()` — uses `Collectors.groupingBy()` to produce a `Map<String, List<BookResponse>>` keyed by author name

**CartService**:
- Maintains a `Map<String, Cart>` (username → cart) using `computeIfAbsent()` for lazy initialization
- `Cart` internally uses `Map.merge()` to add/combine item quantities
- `getCartItems()` iterates `Map.entrySet()` to build the response list

**OrderService**:
- Uses `Queue<BookOrder>` backed by `LinkedList` as a FIFO order queue
- `checkout()` calls `offer()` to enqueue; `processNextOrder()` calls `poll()` to dequeue
- Order items stored in `ArrayList<OrderItem>` preserving insertion order

### 3.4 Security

**Authentication flow**:

```
POST /api/auth/login  ──►  Validate credentials (BCrypt)
                           Generate JWT (HMAC-SHA256, 1h expiry)
                           Return { token: "eyJ..." }
```

**Per-request authorization**:

```
Request with "Authorization: Bearer <token>"
  │
  ▼
JwtAuthFilter
  ├── Extract token from header
  ├── Validate signature + expiration (JwtUtil)
  ├── Load UserDetails from DB
  └── Set SecurityContext authentication
  │
  ▼
SecurityConfig rules evaluate:
  • Public:  GET /api/books/**, GET /api/authors/**, /api/auth/**, static files
  • Authenticated:  /api/cart/**, POST /api/orders/checkout
  • Admin only:  POST/PUT/DELETE /api/books, POST /api/authors,
                 GET /api/orders/pending, POST /api/orders/process-next
```

**JWT token claims**: `sub` (username), `role` (ROLE_USER or ROLE_ADMIN), `iat`, `exp`.

**First-user rule**: The first user to register becomes ADMIN; all subsequent users become USER.

### 3.5 REST API Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Register (first user = admin) |
| POST | `/api/auth/login` | Public | Login, returns JWT |
| GET | `/api/books` | Public | List all books |
| GET | `/api/books/{id}` | Public | Get single book |
| POST | `/api/books` | Admin | Create book |
| PUT | `/api/books/{id}` | Admin | Update book |
| DELETE | `/api/books/{id}` | Admin | Delete book |
| GET | `/api/books/tags` | Public | All unique tags (Set) |
| GET | `/api/books/by-author` | Public | Books grouped by author (Map) |
| GET | `/api/authors` | Public | List all authors |
| GET | `/api/authors/{id}` | Public | Get single author |
| POST | `/api/authors` | Admin | Create author |
| POST | `/api/cart/items` | Auth | Add item to cart |
| GET | `/api/cart` | Auth | View cart contents |
| DELETE | `/api/cart/items/{bookId}` | Auth | Remove item from cart |
| DELETE | `/api/cart` | Auth | Clear cart |
| POST | `/api/orders/checkout` | Auth | Create order from cart |
| GET | `/api/orders/pending` | Admin | View pending order queue |
| POST | `/api/orders/process-next` | Admin | Process next FIFO order |

### 3.6 Error Handling

`GlobalExceptionHandler` catches `ResponseStatusException` and returns:

```json
{ "status": 404, "error": "NOT_FOUND", "message": "Book not found" }
```

Common status codes: 404 (not found), 409 (duplicate ISBN/username), 400 (empty cart), 401 (invalid credentials).

### 3.7 Data Initialization

`data.sql` runs on startup (H2 profile only) and seeds:
- 3 authors: Robert Martin, Martin Fowler, Joshua Bloch
- 3 books with tags: "Clean Code", "Refactoring", "Effective Java"
- No users (first registration becomes admin)

---

## 4. Frontend Architecture

### 4.1 Overview

A **single-page application** using vanilla JavaScript — no frameworks, no build step. All files in `src/main/resources/static/` are served by Spring Boot's default static resource handling.

### 4.2 Module Responsibilities

```
index.html      HTML shell: navbar + <main id="content"> + flash container
css/style.css   Responsive styling with CSS variables
js/
  api.js        HTTP client (fetch wrapper) + JWT token management
  auth.js       Login / register / logout forms + navbar state
  books.js      Book list, create/edit form, add-to-cart, delete
  authors.js    Author list + create form
  cart.js       Cart display, remove items, checkout, clear
  orders.js     Pending orders view + process-next (admin)
  app.js        Router (navigate function) + flash messages + init
```

### 4.3 Routing

`app.js` implements a simple client-side router via a `navigate(page)` switch statement. Clicking nav links calls `navigate('books')`, `navigate('cart')`, etc. Each case replaces `document.getElementById('content').innerHTML` with the rendered HTML for that page — no full page reloads.

Pages: `books`, `authors`, `cart`, `orders`, `login`, `register`, `create-book`, `create-author`.

### 4.4 API Client (api.js)

Central `apiFetch(endpoint, options)` function:

1. Prepends `/api` base path
2. Attaches `Authorization: Bearer <token>` header if a token exists in `localStorage`
3. Handles `204 No Content` (DELETE responses) and empty bodies
4. On `401 Unauthorized`, auto-clears the stored token and updates the navbar
5. Throws `Error` with the server's message for non-OK responses

Token helpers: `getToken()`, `setToken()`, `clearToken()`, `isLoggedIn()`, `isAdmin()` (decodes JWT payload to check role claim).

### 4.5 Authentication State & Navbar

`updateNavBar()` in `auth.js` toggles visibility of nav elements based on login state:

| Element | Visible When |
|---------|-------------|
| Login / Register links | Not logged in |
| Cart link | Logged in |
| Orders link | Logged in + Admin |
| Username display | Logged in |
| Logout link | Logged in |

The username is extracted by base64-decoding the JWT payload (`atob(token.split('.')[1])`).

### 4.6 Conditional UI Rendering

Each page module checks `isLoggedIn()` and `isAdmin()` to show/hide action buttons:

- **Books page**: "Add Book", "Edit", "Delete" buttons — admin only. "Add to Cart" — logged in only.
- **Authors page**: "Add Author" button — admin only.
- **Cart page**: Redirects to login prompt if not authenticated.
- **Orders page**: Only accessible to admins.

### 4.7 XSS Protection

`books.js` defines an `escapeHtml()` function that encodes `&`, `<`, `>`, `"`, `'` to HTML entities before inserting user-generated content into the DOM.

---

## 5. Deployment

### Development (H2)

```bash
mvn spring-boot:run
# App: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:bookstore)
```

Schema is `create-drop` — recreated on every restart. Seed data loaded from `data.sql`.

### Production (Docker + PostgreSQL)

```bash
docker compose up
# App: http://localhost:8080
# PostgreSQL: localhost:5432 (bookstore/bookstore)
```

**Dockerfile** uses a multi-stage build:
1. **Build stage** (`eclipse-temurin:17-jdk`): runs `mvn package -DskipTests`
2. **Runtime stage** (`eclipse-temurin:17-jre`): copies the JAR, exposes port 8080

**Compose** defines two services:
- `postgres` (PostgreSQL 16 Alpine) with a health check and persistent named volume
- `app` (built from Dockerfile) with `SPRING_PROFILES_ACTIVE=docker`, depends on postgres health

The `docker` profile (`application-docker.yml`) switches to PostgreSQL, sets `ddl-auto: update`, and disables H2 console and `data.sql`.

---

## 6. Testing

Tests are in `src/test/java/com/bookstore/` mirroring the main source structure:

| Test Class | Type | What it tests |
|-----------|------|---------------|
| `BookServiceTest` | Unit (Mockito) | Business logic, DTO mapping |
| `AuthorServiceTest` | Unit (Mockito) | Author CRUD logic |
| `BookControllerTest` | Web (@WebMvcTest) | Endpoints, request validation, auth |
| `AuthorControllerTest` | Web (@WebMvcTest) | Author endpoints |
| `AuthControllerTest` | Web (@WebMvcTest) | Login/register flows |
| `BookRepositoryTest` | Data (@DataJpaTest) | Custom query methods |
| `AuthorRepositoryTest` | Data (@DataJpaTest) | Repository operations |
| `JwtUtilTest` | Unit | Token generation, validation, expiry |
| `JwtAuthFilterTest` | Unit | Filter chain behavior |

Tests use JUnit 5 with `@Nested` + `@DisplayName` for organized grouping.

---

## 7. Collections Framework Summary

The application is specifically designed to demonstrate these collections patterns:

| Collection | Location | Pattern Demonstrated |
|-----------|----------|---------------------|
| `Set<String>` | `Book.tags` + `BookService.getAllTags()` | `@ElementCollection`, `HashSet` deduplication |
| `Map<String, List<T>>` | `BookService.getBooksByAuthor()` | `Collectors.groupingBy()` stream grouping |
| `HashMap<Long, Integer>` | `Cart` model | `Map.merge()`, `Map.entrySet()`, `Collections.unmodifiableMap()` |
| `Map<String, Cart>` | `CartService` | `Map.computeIfAbsent()` for lazy init |
| `Queue<BookOrder>` | `OrderService` | `LinkedList` as FIFO queue — `offer()`, `poll()` |
| `ArrayList<OrderItem>` | `BookOrder.items` | Ordered, index-accessible list of line items |

---

## Appendix A — Terms and Abbreviations

| Term / Abbreviation | Definition |
|---------------------|-----------|
| API | Application Programming Interface — a set of endpoints that allow software components to communicate |
| BCrypt | A password-hashing algorithm designed to be computationally expensive, making brute-force attacks impractical |
| CORS | Cross-Origin Resource Sharing — an HTTP mechanism that allows a web page to make requests to a different domain than the one that served it |
| CRUD | Create, Read, Update, Delete — the four basic operations on persistent data |
| CSRF | Cross-Site Request Forgery — an attack where a malicious site tricks a user's browser into performing unwanted actions on another site; disabled here because stateless JWT auth is immune to it |
| CSS | Cascading Style Sheets — a language for describing the visual presentation of HTML documents |
| DDL | Data Definition Language — SQL statements that define database structure (CREATE TABLE, ALTER TABLE, DROP TABLE) |
| DOM | Document Object Model — the browser's in-memory tree representation of an HTML page that JavaScript can manipulate |
| DTO | Data Transfer Object — a plain object used to carry data between layers (e.g., request/response payloads) without exposing internal entities |
| FIFO | First In, First Out — a queue discipline where the earliest added element is processed first |
| H2 | A lightweight, embeddable Java SQL database often used for development and testing |
| HMAC | Hash-based Message Authentication Code — a mechanism for verifying both data integrity and authenticity using a secret key |
| HTML | HyperText Markup Language — the standard markup language for web pages |
| HTTP | HyperText Transfer Protocol — the application-layer protocol used for transmitting web resources |
| JAR | Java Archive — a packaged file format that bundles compiled Java classes and resources into a single distributable file |
| JDK | Java Development Kit — the full development environment for Java, including compiler and tools |
| JJWT | Java JSON Web Token — a third-party library for creating and verifying JWTs in Java |
| JPA | Java Persistence API — a specification for object-relational mapping in Java, implemented here by Hibernate |
| JRE | Java Runtime Environment — a subset of the JDK containing only what is needed to run Java applications |
| JSON | JavaScript Object Notation — a lightweight text format for data exchange |
| JUnit | A widely-used unit testing framework for Java |
| JVM | Java Virtual Machine — the runtime engine that executes Java bytecode |
| JWT | JSON Web Token — a compact, URL-safe token format used for stateless authentication; contains a header, payload, and signature |
| MVC | Model-View-Controller — a design pattern that separates an application into three concerns: data (model), presentation (view), and input handling (controller) |
| Maven | A build automation and dependency management tool for Java projects; configured via `pom.xml` |
| Mockito | A Java mocking framework used in unit tests to replace real dependencies with controlled fakes |
| OAuth2 | Open Authorization 2.0 — an industry-standard protocol for token-based authorization |
| ORM | Object-Relational Mapping — a technique that maps Java objects to relational database tables |
| POJO | Plain Old Java Object — a simple Java class with no framework-specific interfaces or annotations required |
| REST | Representational State Transfer — an architectural style for APIs that uses standard HTTP methods and stateless communication |
| SHA-256 | Secure Hash Algorithm (256-bit) — a cryptographic hash function; used here as part of HMAC-SHA256 for JWT signing |
| SPA | Single-Page Application — a web app that dynamically rewrites the current page instead of loading entire new pages from the server |
| SQL | Structured Query Language — the standard language for querying and manipulating relational databases |
| XSS | Cross-Site Scripting — an attack where malicious scripts are injected into web pages; prevented here by escaping HTML entities in user content |
| `@DataJpaTest` | A Spring Boot test annotation that configures only the JPA layer (repositories + in-memory database) |
| `@ElementCollection` | A JPA annotation that maps a collection of simple values (e.g., `Set<String>`) to a separate database table |
| `@WebMvcTest` | A Spring Boot test annotation that configures only the web layer (controllers + security) without starting the full application |
| `fetch()` | The modern browser API for making HTTP requests from JavaScript, replacing the older `XMLHttpRequest` |
| `localStorage` | A browser API that stores key-value string data persistently across page reloads and browser sessions |
| `pom.xml` | Project Object Model — the Maven configuration file that declares dependencies, plugins, and build settings |
