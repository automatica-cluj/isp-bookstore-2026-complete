// ------------------------------------------------------------------
// books.js — Book list, create, edit, and delete UI
// ------------------------------------------------------------------

async function loadBooks() {
    const content = document.getElementById('content');
    try {
        const books = await apiGetBooks();
        content.innerHTML = renderBookList(books);
    } catch (err) {
        content.innerHTML = '<p class="empty">Failed to load books: ' + err.message + '</p>';
    }
}

function renderBookList(books) {
    const addBtn = isAdmin()
        ? '<a href="#" class="btn btn-primary" onclick="navigate(\'create-book\'); return false;">+ Add Book</a>'
        : '';

    if (books.length === 0) {
        return `
            <div class="toolbar"><h2>Books</h2>${addBtn}</div>
            <p class="empty">No books yet.</p>
        `;
    }

    const rows = books.map(b => {
        const tagsHtml = (b.tags && b.tags.length > 0)
            ? b.tags.map(t => '<span class="tag">' + escapeHtml(t) + '</span>').join(' ')
            : '<span class="text-muted">—</span>';

        return `
            <tr>
                <td>${escapeHtml(b.title)}</td>
                <td>${escapeHtml(b.authorName)}</td>
                <td>${escapeHtml(b.isbn)}</td>
                <td>$${Number(b.price).toFixed(2)}</td>
                <td>${tagsHtml}</td>
                <td class="actions">
                    ${isLoggedIn() ? `<button class="btn btn-primary btn-sm" onclick="handleAddToCart(${b.id})">Add to Cart</button>` : ''}
                    ${isAdmin() ? `
                        <button class="btn btn-primary btn-sm" onclick="navigateEditBook(${b.id})">Edit</button>
                        <button class="btn btn-danger btn-sm" onclick="handleDeleteBook(${b.id})">Delete</button>
                    ` : ''}
                </td>
            </tr>
        `;
    }).join('');

    return `
        <div class="toolbar"><h2>Books</h2>${addBtn}</div>
        <table>
            <thead>
                <tr><th>Title</th><th>Author</th><th>ISBN</th><th>Price</th><th>Tags</th><th></th></tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;
}

async function loadBookForm(bookId) {
    const content = document.getElementById('content');
    try {
        const authors = await apiGetAuthors();
        let book = null;
        if (bookId) {
            book = await apiGetBook(bookId);
        }
        content.innerHTML = renderBookForm(book, authors);

        const form = document.getElementById('book-form');
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (bookId) {
                handleUpdateBook(bookId);
            } else {
                handleCreateBook();
            }
        });
    } catch (err) {
        content.innerHTML = '<p class="empty">Failed to load form: ' + err.message + '</p>';
    }
}

function renderBookForm(book, authors) {
    const isEdit = book !== null;
    const title = isEdit ? 'Edit Book' : 'New Book';
    const tagsValue = isEdit && book.tags ? book.tags.join(', ') : '';

    const authorOptions = authors.map(a =>
        `<option value="${a.id}" ${isEdit && book.authorId === a.id ? 'selected' : ''}>${escapeHtml(a.firstName + ' ' + a.lastName)}</option>`
    ).join('');

    return `
        <div class="form-card">
            <h2>${title}</h2>
            <form id="book-form">
                <div class="form-group">
                    <label for="title">Title</label>
                    <input type="text" id="title" required value="${isEdit ? escapeHtml(book.title) : ''}">
                </div>
                <div class="form-group">
                    <label for="authorId">Author</label>
                    <select id="authorId" required>
                        <option value="">Select an author</option>
                        ${authorOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label for="isbn">ISBN</label>
                    <input type="text" id="isbn" required value="${isEdit ? escapeHtml(book.isbn) : ''}">
                </div>
                <div class="form-group">
                    <label for="price">Price</label>
                    <input type="number" id="price" step="0.01" min="0.01" required value="${isEdit ? book.price : ''}">
                </div>
                <div class="form-group">
                    <label for="tags">Tags (comma-separated)</label>
                    <input type="text" id="tags" placeholder="e.g. programming, bestseller" value="${escapeHtml(tagsValue)}">
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Update' : 'Create'}</button>
                    <button type="button" class="btn" style="background:var(--text-muted);" onclick="navigate('books')">Cancel</button>
                </div>
            </form>
        </div>
    `;
}

async function handleCreateBook() {
    const bookData = readBookForm();
    try {
        await apiCreateBook(bookData);
        showFlash('Book created', 'success');
        navigate('books');
    } catch (err) {
        showFlash('Failed to create book: ' + err.message, 'error');
    }
}

async function handleUpdateBook(id) {
    const bookData = readBookForm();
    try {
        await apiUpdateBook(id, bookData);
        showFlash('Book updated', 'success');
        navigate('books');
    } catch (err) {
        showFlash('Failed to update book: ' + err.message, 'error');
    }
}

async function handleDeleteBook(id) {
    if (!confirm('Delete this book?')) return;
    try {
        await apiDeleteBook(id);
        showFlash('Book deleted', 'success');
        loadBooks();
    } catch (err) {
        showFlash('Failed to delete: ' + err.message, 'error');
    }
}

async function handleAddToCart(bookId) {
    try {
        await apiAddToCart(bookId, 1);
        showFlash('Added to cart', 'success');
    } catch (err) {
        showFlash('Failed to add to cart: ' + err.message, 'error');
    }
}

function navigateEditBook(id) {
    loadBookForm(id);
}

function readBookForm() {
    const tagsInput = document.getElementById('tags').value;
    // Split comma-separated tags, trim whitespace, remove empty strings
    const tags = tagsInput
        ? tagsInput.split(',').map(t => t.trim()).filter(t => t.length > 0)
        : [];

    return {
        title: document.getElementById('title').value,
        authorId: Number(document.getElementById('authorId').value),
        isbn: document.getElementById('isbn').value,
        price: Number(document.getElementById('price').value),
        tags: tags
    };
}

// Prevents XSS: converts special characters like < > & to safe HTML entities
// so user input is displayed as text, not executed as HTML.
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
