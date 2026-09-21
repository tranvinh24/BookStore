document.addEventListener('DOMContentLoaded', async () => {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để xem lịch sử đơn hàng!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
    }, 800);
    return;
  }

  await loadMyOrders();
});

async function loadMyOrders() {
  const loading = document.getElementById('orders-loading');
  const empty = document.getElementById('orders-empty');
  const container = document.getElementById('orders-list-container');

  try {
    const orders = await apiFetch('/api/orders/my');
    loading.style.display = 'none';

    if (!orders || orders.length === 0) {
      empty.style.display = 'block';
      return;
    }

    container.innerHTML = '';
    orders.forEach(order => {
      const card = renderOrderCard(order);
      container.appendChild(card);
    });

  } catch (err) {
    loading.innerHTML = `<p style="color: var(--primary); font-weight: 500;">Không thể tải danh sách đơn hàng: ${err.message}</p>`;
  }
}

function renderOrderCard(order) {
  const card = document.createElement('div');
  card.className = 'order-card';

  // Stepper Calculation
  const steps = ['PENDING', 'PAID', 'PROCESSING', 'SHIPPING', 'DELIVERED'];
  const currentIndex = steps.indexOf(order.status);
  const isCancelled = order.status === 'CANCELLED';

  let stepperHtml = '';
  if (isCancelled) {
    stepperHtml = `
      <div style="background: #FEE2E2; color: #DC2626; padding: 0.75rem 1rem; border-radius: var(--radius-sm); margin-bottom: 1.25rem; font-weight: 600; font-size: 0.9rem; display: flex; align-items: center; gap: 0.5rem;">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>
        Đơn hàng đã bị hủy.
      </div>
    `;
  } else {
    stepperHtml = `
      <div class="stepper">
        <div class="step-item ${currentIndex >= 0 ? (currentIndex === 0 ? 'active' : 'completed') : ''}">
          <div class="step-circle">${currentIndex > 0 ? '✓' : '1'}</div>
          <div class="step-title">Đặt hàng</div>
        </div>
        <div class="step-item ${currentIndex >= 1 ? (currentIndex === 1 ? 'active' : 'completed') : ''}">
          <div class="step-circle">${currentIndex > 1 ? '✓' : '2'}</div>
          <div class="step-title">Thanh toán</div>
        </div>
        <div class="step-item ${currentIndex >= 2 ? (currentIndex === 2 ? 'active' : 'completed') : ''}">
          <div class="step-circle">${currentIndex > 2 ? '✓' : '3'}</div>
          <div class="step-title">Đóng gói</div>
        </div>
        <div class="step-item ${currentIndex >= 3 ? (currentIndex === 3 ? 'active' : 'completed') : ''}">
          <div class="step-circle">${currentIndex > 3 ? '✓' : '4'}</div>
          <div class="step-title">Đang giao</div>
        </div>
        <div class="step-item ${currentIndex >= 4 ? 'completed' : ''}">
          <div class="step-circle">${currentIndex >= 4 ? '✓' : '5'}</div>
          <div class="step-title">Thành công</div>
        </div>
      </div>
    `;
  }

  // Items rows
  let itemsHtml = '';
  const totalQuantity = (order.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0);

  if (order.items && order.items.length > 0) {
    order.items.forEach(item => {
      const subtotal = item.subtotal || (item.priceAtOrder * item.quantity);
      const authorText = item.author ? item.author : 'Tác giả chưa cập nhật';

      itemsHtml += `
        <div class="order-product-card">
          <div class="product-cover-box">
            <img src="/api/books/${item.bookId}/cover?size=thumbnail" 
                 alt="${escapeHtml(item.title)}"
                 class="product-cover-img"
                 onerror="this.parentElement.innerHTML='<div style=\\'height:100%;width:100%;display:flex;align-items:center;justify-content:center;background:var(--bg-subtle);color:var(--text-muted);font-size:1.4rem;\\'>📖</div>'" />
          </div>
          <div class="product-details">
            <a href="/book-detail.html?id=${item.bookId}" class="product-title-link" title="${escapeHtml(item.title)}">
              ${escapeHtml(item.title)}
            </a>
            <div class="product-author-text">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              <span>${escapeHtml(authorText)}</span>
            </div>
            <div class="product-unit-price">
              Đơn giá: <strong class="font-number">${formatPrice(item.priceAtOrder)}</strong>
            </div>
          </div>
          <div class="product-qty-col">
            <span class="product-col-label">Số lượng</span>
            <span class="product-qty-badge font-number">× ${item.quantity}</span>
          </div>
          <div class="product-subtotal-col">
            <span class="product-col-label">Thành tiền</span>
            <span class="product-subtotal-amount font-number">${formatPrice(subtotal)}</span>
          </div>
          <div class="product-action-col">
            <a href="/book-detail.html?id=${item.bookId}" class="btn-view-product">
              Xem sách
            </a>
          </div>
        </div>
      `;
    });
  }

  // Actions if PENDING payment
  let actionHtml = '';
  if (order.status === 'PENDING' && order.payment?.method !== 'COD') {
    actionHtml = `
      <a href="/payment.html?orderId=${order.id}" class="btn btn-primary btn-sm">
        Thanh toán ngay
      </a>
    `;
  }

  card.innerHTML = `
    <div class="order-card-header">
      <div class="order-header-left">
        <div class="order-id-line">
          <span class="order-id-label">MÃ ĐƠN HÀNG</span>
          <span class="order-id-val font-number">#${order.id}</span>
        </div>
        <div class="order-date-line">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          <span>Ngày đặt: ${formatDateTime(order.createdAt)}</span>
        </div>
      </div>

      <div class="order-header-right">
        ${getOrderStatusBadge(order.status)}
      </div>
    </div>

    <div class="order-card-body">
      ${stepperHtml}

      <div class="order-info-grid">
        <div class="order-info-block">
          <div class="order-info-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--primary); flex-shrink: 0;"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
            Địa chỉ nhận hàng
          </div>
          <div class="order-info-content">
            ${order.shippingAddress ? escapeHtml(order.shippingAddress) : 'Chưa cập nhật địa chỉ'}
          </div>
        </div>

        <div class="order-info-block">
          <div class="order-info-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--accent); flex-shrink: 0;"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
            Phương thức thanh toán
          </div>
          <div class="order-info-content" style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
            <span>${getPaymentMethodLabel(order.payment?.method || 'COD')}</span>
            <span>${getPaymentStatusBadge(order.payment?.status)}</span>
          </div>
        </div>

        ${order.trackingNote ? `
          <div class="order-tracking-banner">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;"><rect x="1" y="3" width="15" height="13"></rect><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon><circle cx="5.5" cy="18.5" r="2.5"></circle><circle cx="18.5" cy="18.5" r="2.5"></circle></svg>
            <span><strong>Ghi chú giao hàng:</strong> ${escapeHtml(order.trackingNote)}</span>
          </div>
        ` : ''}
      </div>

      <div class="order-products-section">
        <div class="order-products-header">
          <span class="order-products-title">Sản phẩm đã mua</span>
          <span class="order-products-count">${totalQuantity} sản phẩm</span>
        </div>
        <div class="order-products-list">
          ${itemsHtml}
        </div>
      </div>
    </div>

    <div class="order-card-footer">
      <div class="order-footer-total">
        <span class="order-total-label">Tổng thanh toán:</span>
        <span class="order-total-amount font-number">${formatPrice(order.totalAmount)}</span>
      </div>
      ${actionHtml ? `<div class="order-footer-actions">${actionHtml}</div>` : ''}
    </div>
  `;

  return card;
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}