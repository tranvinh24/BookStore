let currentOrder = null;

document.addEventListener('DOMContentLoaded', async () => {
  if (!isLoggedIn()) {
    window.location.href = '/login.html';
    return;
  }

  const params = new URLSearchParams(window.location.search);
  const orderId = params.get('orderId');

  if (!orderId) {
    showToast('Không tìm thấy thông tin đơn hàng!', 'error');
    setTimeout(() => window.location.href = '/my-orders.html', 1000);
    return;
  }

  await loadOrderInfo(orderId);
  bindEvents(orderId);
});

async function loadOrderInfo(orderId) {
  const loading = document.getElementById('payment-loading');
  const panel = document.getElementById('payment-panel');

  try {
    currentOrder = await apiFetch(`/api/orders/${orderId}`);
    loading.style.display = 'none';
    panel.style.display = 'block';

    document.getElementById('order-id-label').textContent = currentOrder.id;
    document.getElementById('payment-amount').textContent = formatPrice(currentOrder.totalAmount);

    const method = currentOrder.payment?.method || 'VNPAY';
    document.getElementById('payment-method-badge').innerHTML = `
      <span class="ribbon ribbon-lavender">${getPaymentMethodLabel(method)}</span>
    `;

    // If order is already PAID
    if (currentOrder.status === 'PAID' || currentOrder.payment?.status === 'SUCCESS') {
      showPaymentSuccess(currentOrder.payment);
    }

  } catch (err) {
    loading.innerHTML = `<p style="color: var(--ribbon-coral);">Không thể tải thông tin đơn hàng: ${err.message}</p>`;
  }
}

function showPaymentSuccess(paymentData) {
  document.getElementById('simulated-payment-ui').style.display = 'none';
  document.getElementById('payment-action-wrap').style.display = 'none';
  document.getElementById('payment-success-ui').style.display = 'block';

  document.getElementById('transaction-id-text').textContent = paymentData?.transactionId || 'BV-' + Date.now();
  document.getElementById('payment-time-text').textContent = formatDateTime(paymentData?.paidAt || new Date());
}

function bindEvents(orderId) {
  const btnPay = document.getElementById('btn-confirm-pay');

  btnPay.addEventListener('click', async () => {
    btnPay.disabled = true;
    btnPay.textContent = 'Đang xử lý giao dịch...';

    try {
      const res = await apiFetch(`/api/payments/${orderId}/pay`, {
        method: 'POST'
      });

      showToast('Thanh toán đơn hàng thành công!', 'success');
      showPaymentSuccess(res);

    } catch (err) {
      btnPay.disabled = false;
      btnPay.textContent = 'Thử thanh toán lại';
      showToast(err.message || 'Thanh toán không thành công', 'error');
    }
  });
}