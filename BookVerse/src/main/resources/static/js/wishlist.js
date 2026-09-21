document.addEventListener('DOMContentLoaded', async () => {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để xem danh sách yêu thích!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
    }, 800);
    return;
  }

  await loadWishlist();
});

async function loadWishlist() {
  const loading = document.getElementById('wishlist-loading');
  const empty = document.getElementById('wishlist-empty');
  const grid = document.getElementById('wishlist-grid');

  try {
    const items = await apiFetch('/api/wishlist');
    loading.style.display = 'none';

    if (!items || items.length === 0) {
      empty.style.display = 'block';
      grid.innerHTML = '';
      const badge = document.getElementById('nav-wishlist-count');
      if (badge) badge.style.display = 'none';
      return;
    }

    empty.style.display = 'none';
    grid.innerHTML = '';

    items.forEach(book => {
      const bookId = book.bookId || book.id;
      const card = document.createElement('div');
      card.className = 'book-card';
      card.innerHTML = `
        <a href="/book-detail.html?id=${bookId}" class="book-card-cover">
          <img src="/api/books/${bookId}/cover?size=medium" 
               alt="${book.title}" 
               onerror="this.parentElement.innerHTML='<div class=\'book-card-cover-placeholder\' style=\'display:flex;align-items:center;justify-content:center;height:100%;color:var(--text-muted);font-size:2rem;\'>📖</div>'" />
        </a>
        <div class="book-card-content">
          <a href="/book-detail.html?id=${bookId}">
            <h3 class="book-card-title">${book.title}</h3>
          </a>
          <div class="book-card-author">${book.author || 'Tác giả chưa rõ'}</div>
          
          <div class="book-card-bottom">
            <div class="book-price">${formatPrice(book.price)}</div>
            <div class="book-card-actions">
              <button class="btn btn-outline btn-sm btn-remove-wish" data-id="${bookId}" title="Xóa khỏi yêu thích" style="color: var(--primary); border-color: var(--border); display: inline-flex; align-items: center; justify-content: center; padding: 0.4rem 0.6rem;">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
              </button>
              <button class="btn btn-primary btn-sm btn-add-cart" data-id="${bookId}">
                + Giỏ
              </button>
            </div>
          </div>
        </div>
      `;

      grid.appendChild(card);
    });

    // Bind remove button
    document.querySelectorAll('.btn-remove-wish').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await apiFetch(`/api/wishlist/${btn.dataset.id}`, { method: 'DELETE' });
          showToast('Đã xóa khỏi danh sách yêu thích', 'info');
          await loadWishlist();
        } catch (err) {
          showToast(err.message || 'Không thể xóa', 'error');
        }
      });
    });

    // Bind add to cart
    document.querySelectorAll('.btn-add-cart').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await apiFetch('/api/cart', {
            method: 'POST',
            body: { bookId: btn.dataset.id, quantity: 1 }
          });
          showToast('Đã thêm vào giỏ hàng!', 'success');
          updateBadges();
        } catch (err) {
          showToast(err.message || 'Không thể thêm vào giỏ', 'error');
        }
      });
    });

  } catch (err) {
    loading.innerHTML = `<p style="color: var(--ribbon-coral);">Không thể tải danh sách: ${err.message}</p>`;
  }
}