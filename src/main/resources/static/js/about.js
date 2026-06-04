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
        </div>
    `;
}
