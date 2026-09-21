document.addEventListener('DOMContentLoaded', async () => {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để xem giỏ hàng!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
    }, 800);
    return;
  }

  await loadCart();
  bindEvents();
});

async function loadCart() {
  const loading = document.getElementById('cart-loading');
  const empty = document.getElementById('cart-empty');
  const content = document.getElementById('cart-content');

  try {
    const cart = await apiFetch('/api/cart');
    loading.style.display = 'none';

    if (!cart || !cart.items || cart.items.length === 0) {
      empty.style.display = 'block';
      content.style.display = 'none';
      const badge = document.getElementById('nav-cart-count');
      if (badge) badge.style.display = 'none';
      return;
    }

    empty.style.display = 'none';
    content.style.display = 'grid';
    renderCart(cart);

  } catch (err) {
    loading.innerHTML = `<p style="color: var(--ribbon-coral);">Không thể tải giỏ hàng: ${err.message}</p>`;
  }
}

function renderCart(cart) {
  // cập nhật header "Danh sách sản phẩm (N)"
  const countHeader = document.getElementById('cart-count-header');
  if (countHeader) countHeader.textContent = cart.totalItems;

  // cập nhật panel tóm tắt đơn hàng
  const subtotalEl = document.getElementById('summary-subtotal');
  const totalEl    = document.getElementById('summary-total');
  if (subtotalEl) subtotalEl.textContent = formatPrice(cart.totalAmount);
  if (totalEl)    totalEl.textContent    = formatPrice(cart.totalAmount);

  const list = document.getElementById('cart-items-list');
  list.innerHTML = '';

  cart.items.forEach(item => {
    const row = document.createElement('div');
    row.className = 'cart-item-row';
    row.innerHTML = `
      <div class="cart-item-thumb">
        <img src="/api/books/${item.bookId}/cover?size=thumbnail" 
             alt="${item.title}"
             onerror="this.parentElement.innerHTML='<div style=\\'height:100%;display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:0.75rem;\\'>No img</div>'" />
      </div>

      <div>
        <a href="/book-detail.html?id=${item.bookId}" style="font-weight: 600; font-size: 1rem; color: var(--text-main); display: block; margin-bottom: 0.25rem;">
          ${item.title}
        </a>
        <div style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 0.25rem;">${item.author || 'Tác giả chưa rõ'}</div>
        <span class="font-number" style="color: var(--primary); font-weight: 700; font-size: 0.95rem;">${formatPrice(item.price)}</span>
      </div>

      <div class="qty-picker-sm">
        <button class="qty-btn-sm btn-item-minus" data-id="${item.cartItemId}" data-qty="${item.quantity}">-</button>
        <input type="text" class="qty-input-sm" value="${item.quantity}" readonly />
        <button class="qty-btn-sm btn-item-plus" data-id="${item.cartItemId}" data-qty="${item.quantity}">+</button>
      </div>

      <div class="font-number" style="font-weight: 700; font-size: 1.05rem; text-align: right; color: var(--text-main);">
        ${formatPrice(item.subtotal)}
      </div>

      <div style="text-align: right;">
        <button class="btn btn-outline btn-icon btn-sm btn-remove-item" data-id="${item.cartItemId}" title="Xóa khỏi giỏ" style="color: var(--primary); border-color: var(--border); display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0;">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
        </button>
      </div>
    `;

    list.appendChild(row);
  });

  // Bind item buttons
  document.querySelectorAll('.btn-item-minus').forEach(btn => {
    btn.addEventListener('click', async () => {
      const itemId = btn.dataset.id;
      const currentQty = parseInt(btn.dataset.qty);
      if (currentQty > 1) {
        await updateCartItemQty(itemId, currentQty - 1);
      } else {
        await removeCartItem(itemId);
      }
    });
  });

  document.querySelectorAll('.btn-item-plus').forEach(btn => {
    btn.addEventListener('click', async () => {
      const itemId = btn.dataset.id;
      const currentQty = parseInt(btn.dataset.qty);
      await updateCartItemQty(itemId, currentQty + 1);
    });
  });

  document.querySelectorAll('.btn-remove-item').forEach(btn => {
    btn.addEventListener('click', async () => {
      await removeCartItem(btn.dataset.id);
    });
  });
}

async function updateCartItemQty(itemId, newQty) {
  try {
    await apiFetch(`/api/cart/${itemId}`, {
      method: 'PUT',
      body: { quantity: newQty }
    });
    await loadCart();
    updateBadges();
  } catch (err) {
    showToast(err.message || 'Không thể cập nhật số lượng', 'error');
  }
}

async function removeCartItem(itemId) {
  try {
    await apiFetch(`/api/cart/${itemId}`, { method: 'DELETE' });
    showToast('Đã xóa sản phẩm khỏi giỏ hàng', 'info');
    await loadCart();
    updateBadges();
  } catch (err) {
    showToast(err.message || 'Không thể xóa sản phẩm', 'error');
  }
}

function bindEvents() {
  document.getElementById('btn-clear-cart').addEventListener('click', async () => {
    if (!confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?')) return;
    try {
      await apiFetch('/api/cart', { method: 'DELETE' });
      showToast('Đã xóa sạch giỏ hàng', 'info');
      await loadCart();
      updateBadges();
    } catch (err) {
      showToast(err.message || 'Không thể xóa giỏ hàng', 'error');
    }
  });
}