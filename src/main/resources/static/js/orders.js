// ------------------------------------------------------------------
// orders.js — Order queue UI
// ------------------------------------------------------------------

async function loadOrders() {
    const content = document.getElementById('content');
    try {
        const orders = await apiGetPendingOrders();
        content.innerHTML = renderOrders(orders);
    } catch (err) {
        content.innerHTML = '<p class="empty">Failed to load orders: ' + err.message + '</p>';
    }
}

function renderOrders(orders) {
    const processBtn = isAdmin()
        ? '<button class="btn btn-primary" onclick="handleProcessNext()">Process Next Order</button>'
        : '';

    if (!orders || orders.length === 0) {
        return `
            <div class="toolbar"><h2>Pending Orders</h2></div>
            <p class="empty">No pending orders in the queue.</p>
        `;
    }

    const cards = orders.map(order => {
        const itemRows = order.items.map(item => `
            <tr>
                <td>${escapeHtml(item.title)}</td>
                <td>${item.quantity}</td>
                <td>$${Number(item.unitPrice).toFixed(2)}</td>
                <td>$${Number(item.subtotal).toFixed(2)}</td>
            </tr>
        `).join('');

        return `
            <div class="order-card">
                <div class="order-header">
                    <strong>Order #${order.id}</strong>
                    <span class="text-muted">${new Date(order.placedAt).toLocaleString()}</span>
                    <span class="tag">${order.status}</span>
                </div>
                <table>
                    <thead>
                        <tr><th>Title</th><th>Qty</th><th>Price</th><th>Subtotal</th></tr>
                    </thead>
                    <tbody>${itemRows}</tbody>
                </table>
            </div>
        `;
    }).join('');

    return `
        <div class="toolbar"><h2>Pending Orders (${orders.length})</h2>${processBtn}</div>
        ${cards}
    `;
}

async function handleProcessNext() {
    try {
        const processed = await apiProcessNextOrder();
        showFlash('Order #' + processed.id + ' processed', 'success');
        loadOrders();
    } catch (err) {
        showFlash('Failed: ' + err.message, 'error');
    }
}
