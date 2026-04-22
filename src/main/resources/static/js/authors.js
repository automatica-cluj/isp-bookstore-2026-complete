// ------------------------------------------------------------------
// authors.js — Author list and create UI
// ------------------------------------------------------------------

async function loadAuthors() {
    const content = document.getElementById('content');
    try {
        const authors = await apiGetAuthors();
        content.innerHTML = renderAuthorList(authors);
    } catch (err) {
        content.innerHTML = '<p class="empty">Failed to load authors: ' + err.message + '</p>';
    }
}

function renderAuthorList(authors) {
    const addBtn = isAdmin()
        ? '<a href="#" class="btn btn-primary" onclick="navigate(\'create-author\'); return false;">+ Add Author</a>'
        : '';

    if (authors.length === 0) {
        return `
            <div class="toolbar"><h2>Authors</h2>${addBtn}</div>
            <p class="empty">No authors yet.</p>
        `;
    }

    const rows = authors.map(a => `
        <tr>
            <td>${escapeHtml(a.firstName)}</td>
            <td>${escapeHtml(a.lastName)}</td>
        </tr>
    `).join('');

    return `
        <div class="toolbar"><h2>Authors</h2>${addBtn}</div>
        <table>
            <thead>
                <tr><th>First Name</th><th>Last Name</th></tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;
}

function loadAuthorForm() {
    const content = document.getElementById('content');
    content.innerHTML = `
        <div class="form-card">
            <h2>New Author</h2>
            <form id="author-form">
                <div class="form-group">
                    <label for="firstName">First Name</label>
                    <input type="text" id="firstName" required>
                </div>
                <div class="form-group">
                    <label for="lastName">Last Name</label>
                    <input type="text" id="lastName" required>
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Create</button>
                    <button type="button" class="btn" style="background:var(--text-muted);" onclick="navigate('authors')">Cancel</button>
                </div>
            </form>
        </div>
    `;

    document.getElementById('author-form').addEventListener('submit', async function (e) {
        e.preventDefault();
        const authorData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value
        };
        try {
            await apiCreateAuthor(authorData);
            showFlash('Author created', 'success');
            navigate('authors');
        } catch (err) {
            showFlash('Failed to create author: ' + err.message, 'error');
        }
    });
}
