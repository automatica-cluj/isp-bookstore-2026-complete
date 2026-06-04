// ------------------------------------------------------------------
// about.js — Static About page
// ------------------------------------------------------------------

function renderAbout() {
    return `
        <div class="form-card">
            <h2>About Bookstore</h2>
            <p>
                Bookstore is a reference application for the
                <strong>Software Engineering (ISP) 2026</strong> course at UTCN.
                It demonstrates the Java Collections Framework inside a realistic
                Spring Boot app with JWT authentication and a vanilla-JS single-page
                frontend.
            </p>
            <p class="text-muted">
                Browse the catalog, manage authors and books, add items to your cart,
                and place orders — all backed by Set, Map, Queue, and List patterns
                in the service layer.
            </p>

            <h2>Pages</h2>
            <ul class="about-pages">
                <li>
                    <strong>Books</strong> — the catalog of every book, with author,
                    ISBN, price, and tags. Add titles to your cart; admins can create,
                    edit, and delete books.
                </li>
                <li>
                    <strong>Authors</strong> — the list of authors behind the catalog.
                    Admins can add new ones.
                </li>
                <li>
                    <strong>Cart</strong> — review the items you've added, adjust or
                    remove them, and place your order.
                </li>
                <li>
                    <strong>Orders</strong> — the queue of placed orders. Admins process
                    them one at a time, in the order they arrived.
                </li>
                <li>
                    <strong>Login / Register</strong> — sign in to an existing account or
                    create a new one to start shopping.
                </li>
            </ul>
        </div>
    `;
}
