// ------------------------------------------------------------------
// auth.js — Login, Register, and Logout UI
// ------------------------------------------------------------------

function renderLoginForm() {
    return `
        <div class="form-card">
            <h2>Login</h2>
            <form id="login-form">
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" required minlength="3">
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" required minlength="6">
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Login</button>
                </div>
            </form>
        </div>
    `;
}

function renderRegisterForm() {
    return `
        <div class="form-card">
            <h2>Register</h2>
            <form id="register-form">
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" required minlength="3" maxlength="50">
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" required minlength="6" maxlength="100">
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Register</button>
                </div>
            </form>
        </div>
    `;
}

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        await apiLogin(username, password);
        updateNavBar();
        showFlash('Logged in as ' + username, 'success');
        navigate('books');
    } catch (err) {
        showFlash('Login failed: ' + err.message, 'error');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        await apiRegister(username, password);
        updateNavBar();
        showFlash('Registered and logged in as ' + username, 'success');
        navigate('books');
    } catch (err) {
        showFlash('Registration failed: ' + err.message, 'error');
    }
}

function handleLogout() {
    clearToken();
    updateNavBar();
    showFlash('Logged out', 'success');
    navigate('books');
}

function updateNavBar() {
    const loggedIn = isLoggedIn();
    document.getElementById('nav-login').style.display = loggedIn ? 'none' : '';
    document.getElementById('nav-register').style.display = loggedIn ? 'none' : '';
    document.getElementById('nav-logout').style.display = loggedIn ? '' : 'none';
    document.getElementById('nav-cart').style.display = loggedIn ? '' : 'none';
    document.getElementById('nav-orders').style.display = (loggedIn && isAdmin()) ? '' : 'none';

    const userSpan = document.getElementById('nav-user');
    if (loggedIn) {
        // A JWT has 3 parts separated by dots: header.payload.signature
        // We split on '.', take the payload (index 1), base64-decode it, and parse the JSON
        try {
            const payload = JSON.parse(atob(getToken().split('.')[1]));
            userSpan.textContent = payload.sub;
            userSpan.style.display = '';
        } catch (e) {
            userSpan.style.display = 'none';
        }
    } else {
        userSpan.style.display = 'none';
    }
}
