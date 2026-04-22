// ------------------------------------------------------------------
// api.js — REST API client with JWT token management
// ------------------------------------------------------------------

const API_BASE = '/api';

// --- Token helpers (localStorage) ---

function getToken() {
    return localStorage.getItem('jwt_token');
}

function setToken(token) {
    localStorage.setItem('jwt_token', token);
}

function clearToken() {
    localStorage.removeItem('jwt_token');
}

function isLoggedIn() {
    return getToken() !== null;
}

function isAdmin() {
    try {
        var payload = JSON.parse(atob(getToken().split('.')[1]));
        return payload.role === 'ROLE_ADMIN';
    } catch (e) {
        return false;
    }
}

// --- Generic fetch wrapper ---

async function apiFetch(endpoint, options = {}) {
    const url = API_BASE + endpoint;

    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const response = await fetch(url, {
        method: options.method || 'GET',
        headers: headers,
        body: options.body || undefined
    });

    // 204 No Content (e.g. successful DELETE) — nothing to parse
    if (response.status === 204) return null;

    // 200 with empty body (e.g. cart add returns 200 OK with no JSON)
    const text = await response.text();
    if (!text) return null;

    const body = JSON.parse(text);

    if (!response.ok) {
        if (response.status === 401) {
            clearToken();
            updateNavBar();
        }
        throw new Error(body.message || response.statusText);
    }

    return body;
}

// --- Auth API ---

async function apiLogin(username, password) {
    const data = await apiFetch('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
    });
    if (data && data.token) {
        setToken(data.token);
    }
    return data;
}

async function apiRegister(username, password) {
    const data = await apiFetch('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, password })
    });
    if (data && data.token) {
        setToken(data.token);
    }
    return data;
}

// --- Books API ---

async function apiGetBooks() {
    return apiFetch('/books');
}

async function apiGetBook(id) {
    return apiFetch('/books/' + id);
}

async function apiCreateBook(bookData) {
    return apiFetch('/books', {
        method: 'POST',
        body: JSON.stringify(bookData)
    });
}

async function apiUpdateBook(id, bookData) {
    return apiFetch('/books/' + id, {
        method: 'PUT',
        body: JSON.stringify(bookData)
    });
}

async function apiDeleteBook(id) {
    return apiFetch('/books/' + id, {
        method: 'DELETE'
    });
}

// --- Authors API ---

async function apiGetAuthors() {
    return apiFetch('/authors');
}

async function apiCreateAuthor(authorData) {
    return apiFetch('/authors', {
        method: 'POST',
        body: JSON.stringify(authorData)
    });
}

// --- Tags API ---

async function apiGetAllTags() {
    return apiFetch('/books/tags');
}

async function apiGetBooksByAuthor() {
    return apiFetch('/books/by-author');
}

// --- Cart API ---

async function apiAddToCart(bookId, quantity) {
    return apiFetch('/cart/items', {
        method: 'POST',
        body: JSON.stringify({ bookId, quantity })
    });
}

async function apiGetCart() {
    return apiFetch('/cart');
}

async function apiRemoveFromCart(bookId) {
    return apiFetch('/cart/items/' + bookId, {
        method: 'DELETE'
    });
}

async function apiClearCart() {
    return apiFetch('/cart', {
        method: 'DELETE'
    });
}

// --- Orders API ---

async function apiCheckout() {
    return apiFetch('/orders/checkout', {
        method: 'POST'
    });
}

async function apiGetPendingOrders() {
    return apiFetch('/orders/pending');
}

async function apiProcessNextOrder() {
    return apiFetch('/orders/process-next', {
        method: 'POST'
    });
}
