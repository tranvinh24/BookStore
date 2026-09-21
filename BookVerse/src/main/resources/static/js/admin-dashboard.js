document.addEventListener('DOMContentLoaded', async () => {
  if (!isAdmin()) {
    showToast('Khu vực dành riêng cho Quản trị viên!', 'error');
    setTimeout(() => window.location.href = '/index.html', 800);
    return;
  }

  await Promise.all([loadRevenueStats(), loadLowStockBooks(), loadRecentOrders()]);
});

async function loadRevenueStats() {
  try {
    const [monthly, daily] = await Promise.all([
      apiFetch('/api/admin/revenue/monthly'),
      apiFetch('/api/admin/revenue/daily')
    ]);

    document.getElementById('stat-monthly-revenue').textContent = formatPrice(monthly.totalRevenue);
    document.getElementById('stat-month-period').textContent = `Kỳ: ${monthly.period}`;
    document.getElementById('stat-total-orders').textContent = monthly.totalOrders;
    document.getElementById('stat-pending-orders').textContent = monthly.pendingOrders;

    document.getElementById('stat-daily-revenue').textContent = formatPrice(daily.totalRevenue);
    document.getElementById('stat-daily-paid').textContent = `${daily.paidOrders} đơn thanh toán hôm nay`;

  } catch (err) {
    showToast('Lỗi tải thống kê doanh thu: ' + err.message, 'error');
  }
}

async function loadLowStockBooks() {
  try {
    const books = await apiFetch('/api/admin/books/low-stock?threshold=5');
    const panel = document.getElementById('low-stock-panel');
    const list = document.getElementById('low-stock-list');

    if (!books || books.length === 0) {
      panel.style.display = 'none';
      return;
    }

    panel.style.display = 'block';
    document.getElementById('low-stock-count').textContent = books.length;
    list.innerHTML = '';

    books.forEach(b => {
      const item = document.createElement('div');
      item.style.cssText = 'background: var(--paper-warm); padding: 0.6rem 0.8rem; border-radius: var(--radius-sm); font-size: 0.85rem; display: flex; justify-content: space-between; align-items: center;';
      item.innerHTML = `
        <div style="font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 150px;" title="${b.title}">
          ${b.title}
        </div>
        <span class="ribbon ribbon-coral font-number">Còn ${b.stock}</span>
      `;
      list.appendChild(item);
    });

  } catch (e) {}
}

async function loadRecentOrders() {
  const tbody = document.getElementById('recent-orders-table-body');
  try {
    const orders = await apiFetch('/api/admin/orders');
    if (!orders || orders.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;">Chưa có đơn hàng nào phát sinh.</td></tr>';
      return;
    }

    tbody.innerHTML = '';
    const recent = orders.slice(0, 5);

    recent.forEach(o => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="font-number" style="font-weight: 600; font-size: 0.82rem;">${o.id.substring(0, 8)}...</td>
        <td><strong>${o.userName || o.userId}</strong></td>
        <td style="font-size: 0.85rem; color: var(--ink-muted);">${formatDateTime(o.createdAt)}</td>
        <td class="font-number" style="font-weight: 700;">${formatPrice(o.totalAmount)}</td>
        <td>${getOrderStatusBadge(o.status)}</td>
        <td>
          <a href="/admin/orders.html?id=${o.id}" class="btn btn-outline btn-sm">Chi tiết</a>
        </td>
      `;
      tbody.appendChild(tr);
    });

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--ribbon-coral);">${err.message}</td></tr>`;
  }
}