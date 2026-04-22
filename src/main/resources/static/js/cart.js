// ------------------------------------------------------------------
// cart.js — Shopping cart UI
// ------------------------------------------------------------------

async function loadCart() {
    const content = document.getElementById('content');
    if (!isLoggedIn()) {
        content.innerHTML = '<p class="empty">Please <a href="#" onclick="navigate(\'login\'); return false;">log in</a> to view your cart.</p>';
        return;
    }
    try {
        const items = await apiGetCart();
        content.innerHTML = renderCart(items);
    } catch (err) {
        content.innerHTML = '<p class="empty">Failed to load cart: ' + err.message + '</p>';
    }
}

function renderCart(items) {
    if (!items || items.length === 0) {
        return `
            <div class="toolbar"><h2>Shopping Cart</h2></div>
            <p class="empty">Your cart is empty.</p>
        `;
    }

    // Calculate total from all subtotals
    let total = 0;
    for (let i = 0; i < items.length; i++) {
        total += Number(items[i].subtotal);
    }

    const rows = items.map(item => `
        <tr>
            <td>${escapeHtml(item.title)}</td>
            <td>${item.quantity}</td>
            <td>$${Number(item.unitPrice).toFixed(2)}</td>
            <td>$${Number(item.subtotal).toFixed(2)}</td>
            <td class="actions">
                <button class="btn btn-danger btn-sm" onclick="handleRemoveFromCart(${item.bookId})">Remove</button>
            </td>
        </tr>
    `).join('');

    return `
        <div class="toolbar">
            <h2>Shopping Cart</h2>
            <div class="actions">
                <button class="btn btn-primary" onclick="handleCheckout()">Checkout</button>
                <button class="btn btn-danger" onclick="handleClearCart()">Clear Cart</button>
            </div>
        </div>
        <table>
            <thead>
                <tr><th>Title</th><th>Qty</th><th>Price</th><th>Subtotal</th><th></th></tr>
            </thead>
            <tbody>${rows}</tbody>
            <tfoot>
                <tr><td colspan="3" style="text-align:right;font-weight:bold;">Total:</td><td style="font-weight:bold;">$${total.toFixed(2)}</td><td></td></tr>
            </tfoot>
        </table>
    `;
}

async function handleRemoveFromCart(bookId) {
    try {
        await apiRemoveFromCart(bookId);
        showFlash('Item removed', 'success');
        loadCart();
    } catch (err) {
        showFlash('Failed to remove item: ' + err.message, 'error');
    }
}

async function handleClearCart() {
    try {
        await apiClearCart();
        showFlash('Cart cleared', 'success');
        loadCart();
    } catch (err) {
        showFlash('Failed to clear cart: ' + err.message, 'error');
    }
}

async function handleCheckout() {
    try {
        await apiCheckout();
        showFlash('Order placed!', 'success');
        loadCart();
    } catch (err) {
        showFlash('Checkout failed: ' + err.message, 'error');
    }
}
