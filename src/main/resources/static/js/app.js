// ------------------------------------------------------------------
// app.js — Page controller and initialization
// ------------------------------------------------------------------

function navigate(page) {
    const content = document.getElementById('content');

    switch (page) {
        case 'books':
            loadBooks();
            break;
        case 'authors':
            loadAuthors();
            break;
        case 'cart':
            loadCart();
            break;
        case 'orders':
            loadOrders();
            break;
        case 'login':
            content.innerHTML = renderLoginForm();
            document.getElementById('login-form').addEventListener('submit', handleLogin);
            break;
        case 'register':
            content.innerHTML = renderRegisterForm();
            document.getElementById('register-form').addEventListener('submit', handleRegister);
            break;
        case 'create-book':
            loadBookForm(null);
            break;
        case 'create-author':
            loadAuthorForm();
            break;
        default:
            loadBooks();
    }
}

// --- Flash message helper ---

function showFlash(message, type) {
    const el = document.getElementById('flash');
    el.textContent = message;
    el.className = 'flash ' + type;
    el.style.display = 'block';
    setTimeout(function () {
        el.style.display = 'none';
    }, 3000);
}

// --- Init ---

document.addEventListener('DOMContentLoaded', function () {
    updateNavBar();
    navigate('books');
});
