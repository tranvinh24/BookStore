let currentFilterStatus = '';

document.addEventListener('DOMContentLoaded', async () => {
  if (!isAdmin()) {
    showToast('Khu vực dành riêng cho Quản trị viên!', 'error');
    setTimeout(() => window.location.href = '/index.html', 800);
    return;
  }

  await loadOrders();
  bindEvents();
});

async function loadOrders() {
  const tbody = document.getElementById('orders-table-body');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">Đang tải danh sách...</td></tr>';

  try {
    const endpoint = currentFilterStatus 
      ? `/api/admin/orders?status=${currentFilterStatus}&page=0&size=200`
      : '/api/admin/orders?page=0&size=200';

    const data = await apiFetch(endpoint);
    const orders = Array.isArray(data) ? data : (data?.content || []);

    if (!orders || orders.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">Không có đơn hàng nào phù hợp bộ lọc.</td></tr>';
      return;
    }

    tbody.innerHTML = '';
    orders.forEach(o => {
      const displayId = o.id ? (o.id.length > 8 ? o.id.substring(0, 8) + '...' : o.id) : '—';
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="font-number" style="font-weight: 600; font-size: 0.85rem;" title="${o.id || ''}">${displayId}</td>
        <td>
          <div style="font-weight: 600; color: var(--ink);">${o.userName || 'Ẩn danh'}</div>
          <div style="font-size: 0.78rem; color: var(--ink-muted);">${o.itemCount || 1} sản phẩm</div>
        </td>
        <td style="max-width: 220px; font-size: 0.85rem;">
          <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${o.shippingAddress || ''}">
            ${o.shippingAddress || 'Chưa cập nhật'}
          </div>
          ${o.trackingNote ? `<div style="color: var(--primary); font-size: 0.78rem; margin-top: 0.15rem;">Vận đơn: ${o.trackingNote}</div>` : ''}
        </td>
        <td class="font-number" style="font-weight: 700;">${formatPrice(o.totalAmount)}</td>
        <td>${getOrderStatusBadge(o.status)}</td>
        <td style="font-size: 0.82rem; color: var(--ink-muted);">${formatDateTime(o.createdAt)}</td>
        <td style="text-align: right;">
          <button class="btn btn-primary btn-sm btn-update-status" 
                  data-id="${o.id}" 
                  data-status="${o.status}" 
                  data-method="${o.paymentMethod || 'COD'}"
                  data-note="${o.trackingNote || ''}"
                  ${o.status === 'DELIVERED' || o.status === 'CANCELLED' ? 'disabled' : ''}>
            Xử lý
          </button>
        </td>
      `;
      tbody.appendChild(tr);
    });

    // Bind Update Status Button
    document.querySelectorAll('.btn-update-status').forEach(btn => {
      btn.addEventListener('click', () => {
        openStatusModal(btn.dataset.id, btn.dataset.status, btn.dataset.note, btn.dataset.method);
      });
    });

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--ribbon-coral);">${err.message}</td></tr>`;
  }
}

function openStatusModal(orderId, currentStatus, note, method) {
  document.getElementById('status-order-id').value = orderId;
  document.getElementById('status-order-id-label').textContent = orderId;
  document.getElementById('input-tracking-note').value = note || '';
  document.getElementById('status-modal-error').classList.remove('show');

  // Pre-select next logical status
  const select = document.getElementById('select-next-status');
  if (currentStatus === 'PENDING') {
    select.value = (method === 'COD') ? 'PROCESSING' : 'PAID';
  }
  else if (currentStatus === 'PAID') select.value = 'PROCESSING';
  else if (currentStatus === 'PROCESSING') select.value = 'SHIPPING';
  else if (currentStatus === 'SHIPPING') select.value = 'DELIVERED';
  else select.value = currentStatus;

  document.getElementById('status-modal').classList.add('show');
}

function bindEvents() {
  // Tabs click
  document.getElementById('order-status-filter-tabs').addEventListener('click', (e) => {
    const btn = e.target.closest('button');
    if (!btn) return;

    document.querySelectorAll('#order-status-filter-tabs button').forEach(b => {
      b.className = 'btn btn-outline btn-sm';
    });
    btn.className = 'btn btn-secondary btn-sm active';

    currentFilterStatus = btn.dataset.status;
    loadOrders();
  });

  // Modal close
  document.getElementById('btn-close-status-modal').addEventListener('click', () => {
    document.getElementById('status-modal').classList.remove('show');
  });
  document.getElementById('btn-cancel-status-modal').addEventListener('click', () => {
    document.getElementById('status-modal').classList.remove('show');
  });

  // Submit Status Form
  document.getElementById('status-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const orderId = document.getElementById('status-order-id').value;
    const status = document.getElementById('select-next-status').value;
    const trackingNote = document.getElementById('input-tracking-note').value.trim();

    const errEl = document.getElementById('status-modal-error');
    errEl.classList.remove('show');

    const btnSubmit = document.getElementById('btn-submit-status');
    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Đang cập nhật...';

    try {
      await apiFetch(`/api/admin/orders/${orderId}/status`, {
        method: 'PUT',
        body: {
          status,
          trackingNote: trackingNote || undefined
        }
      });

      showToast('Cập nhật trạng thái đơn hàng thành công!', 'success');
      document.getElementById('status-modal').classList.remove('show');
      loadOrders();

    } catch (err) {
      errEl.textContent = err.message || 'Chuyển trạng thái không hợp lệ.';
      errEl.classList.add('show');
    } finally {
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Cập nhật';
    }
  });
}