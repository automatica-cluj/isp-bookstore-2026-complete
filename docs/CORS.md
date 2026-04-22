# CORS — Cross-Origin Resource Sharing

## What is CORS?

When a web page makes a request to a different **origin** (a different domain, port, or protocol), the browser blocks it by default. This is called the **Same-Origin Policy** — a fundamental browser security mechanism that prevents a malicious site from reading data from another site where you might be logged in.

An **origin** is the combination of **scheme + host + port**:

| URL | Origin |
|-----|--------|
| `http://localhost:8090` | `http://localhost:8090` |
| `https://localhost` | `https://localhost:443` |
| `https://my-app.duckdns.org` | `https://my-app.duckdns.org:443` |

Two URLs have the **same origin** only if all three parts match. If any part differs, the browser treats it as a **cross-origin** request.

### Examples

| Page loaded from | Request to | Same origin? | Why |
|-----------------|-----------|-------------|-----|
| `http://localhost:8090` | `http://localhost:8090/api/books` | Yes | Everything matches |
| `https://localhost` | `https://localhost/api/books` | Yes | Everything matches |
| `http://localhost:3000` | `http://localhost:8090/api/books` | No | Port differs (3000 vs 8090) |
| `http://localhost:8090` | `https://localhost/api/books` | No | Scheme differs (http vs https) |
| `https://my-app.duckdns.org` | `https://my-app.duckdns.org/api/books` | Yes | Everything matches |

### When does CORS matter?

CORS only applies to **browser requests**. Tools like `curl`, Postman, or server-to-server calls are not affected — they do not enforce the Same-Origin Policy.

---

## How CORS Works

When a browser detects a cross-origin request, it asks the server for permission using HTTP headers.

### Simple Requests

For basic GET requests, the browser sends the request and checks the response for a `Access-Control-Allow-Origin` header:

```
Browser → Server:
  GET /api/books
  Origin: https://localhost

Server → Browser:
  200 OK
  Access-Control-Allow-Origin: https://localhost

Browser: ✅ Origin is allowed, show the response to JavaScript
```

If the header is missing or doesn't match, the browser blocks the response:

```
Server → Browser:
  200 OK
  (no Access-Control-Allow-Origin header)

Browser: ❌ CORS error — response blocked
```

### Preflight Requests

For requests with custom headers (like `Authorization`) or methods other than GET/POST, the browser sends a **preflight** — an `OPTIONS` request — before the actual request:

```
1. Browser → Server:
     OPTIONS /api/books
     Origin: https://localhost
     Access-Control-Request-Method: POST
     Access-Control-Request-Headers: Authorization, Content-Type

2. Server → Browser:
     200 OK
     Access-Control-Allow-Origin: https://localhost
     Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
     Access-Control-Allow-Headers: Authorization, Content-Type

3. Browser: ✅ Preflight passed, now send the actual request

4. Browser → Server:
     POST /api/books
     Origin: https://localhost
     Authorization: Bearer eyJhbG...
     Content-Type: application/json
```

If the preflight fails (403 or missing headers), the browser never sends the actual request.

---

## CORS in This Application

### Configuration

CORS is configured in `src/main/java/com/bookstore/config/WebConfig.java`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    "http://localhost:*",
                    "https://localhost:*",
                    "https://localhost",
                    "https://*.duckdns.org"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }
}
```

### What Each Line Means

| Setting | Value | Purpose |
|---------|-------|---------|
| `addMapping` | `/api/**` | Apply CORS rules only to API endpoints |
| `allowedOriginPatterns` | `http://localhost:*`, etc. | Which origins are allowed to make requests |
| `allowedMethods` | `GET, POST, PUT, DELETE, OPTIONS` | Which HTTP methods are permitted |
| `allowedHeaders` | `Authorization, Content-Type` | Which request headers are permitted |
| `allowCredentials` | `true` | Allow cookies and Authorization headers |

### Why Multiple Origins?

The application can be accessed in different ways depending on the environment:

| Environment | Origin | Pattern |
|-------------|--------|---------|
| Local dev (no Caddy) | `http://localhost:8090` | `http://localhost:*` |
| Local dev (with Caddy) | `https://localhost` | `https://localhost`, `https://localhost:*` |
| Remote (DuckDNS) | `https://my-app.duckdns.org` | `https://*.duckdns.org` |

### When Does CORS Apply Here?

With Caddy as a reverse proxy, both the frontend (HTML/JS) and the API are served from the **same origin** through Caddy. In this case, all API requests are same-origin and CORS does not apply.

CORS becomes relevant when:

- The frontend is served from a different origin than the API (e.g. a React dev server on port 3000 calling the API on port 8090)
- You access the API directly from a page loaded from a different URL

---

## Common CORS Errors and Fixes

### "Invalid CORS request" (403)

```
Registration failed: Unexpected token 'I', "Invalid CORS request" is not valid JSON
```

**Cause:** The request origin is not in the `allowedOriginPatterns` list. Spring rejects the request with a plain-text 403 response, and the JavaScript fails when trying to parse it as JSON.

**Fix:** Add the origin to `allowedOriginPatterns` in `WebConfig.java`. Pay attention to the **scheme** — `http://localhost` and `https://localhost` are different origins.

### "No 'Access-Control-Allow-Origin' header"

```
Access to fetch at 'http://localhost:8090/api/books' from origin 'http://localhost:3000'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present
```

**Cause:** The server did not include the CORS response header. Either the origin is not allowed, or CORS is not configured for that endpoint.

**Fix:** Ensure `addMapping` covers the endpoint path and the origin is listed.

### "Request header field Authorization is not allowed"

**Cause:** The `Authorization` header is not in `allowedHeaders`.

**Fix:** Add `"Authorization"` to the `.allowedHeaders(...)` list.

---

## CORS vs. Reverse Proxy

A reverse proxy like Caddy can eliminate CORS issues entirely by serving everything from a single origin:

**Without reverse proxy** — CORS required:
```
Browser loads page from:  http://localhost:3000  (React dev server)
API call goes to:         http://localhost:8090  (Spring Boot)
→ Different ports → cross-origin → CORS needed
```

**With reverse proxy** — no CORS needed:
```
Browser loads page from:  https://my-app.duckdns.org  (Caddy)
API call goes to:         https://my-app.duckdns.org  (Caddy → Spring Boot)
→ Same origin → no CORS
```

This is one of the practical benefits of using a reverse proxy in production — it simplifies the security model by keeping everything on the same origin.

---

## Terms

| Term | Definition |
|------|-----------|
| CORS | Cross-Origin Resource Sharing — a browser mechanism that allows controlled access to resources from a different origin |
| Origin | The combination of scheme (http/https), host, and port that identifies where a page was loaded from |
| Preflight | An `OPTIONS` request the browser sends before certain cross-origin requests to check if the server allows them |
| Same-Origin Policy | A browser security rule that prevents JavaScript from reading responses from a different origin |
| Reverse proxy | A server that forwards requests to another server — when both frontend and API go through the same proxy, requests become same-origin |
