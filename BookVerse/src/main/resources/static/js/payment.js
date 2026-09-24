let currentOrderId = null;

document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search);

  // Trường hợp 1: VNPay redirect trở lại kèm kết quả giao dịch
  if (params.has('vnp_ResponseCode')) {
    await handleVNPayCallbackReturn();
    return;
  }

  // Trường hợp 2: Khách hàng truy cập để thanh toán đơn hàng (?orderId=...)
  if (!isLoggedIn()) {
    window.location.href = '/login.html';
    return;
  }

  currentOrderId = params.get('orderId');
  if (!currentOrderId) {
    showToast('Không tìm thấy thông tin đơn hàng cần thanh toán!', 'error');
    setTimeout(() => window.location.href = '/my-orders.html', 1000);
    return;
  }

  await loadOrderInfo(currentOrderId);
  bindEvents();
});

/**
 * Xử lý khi VNPay chuyển hướng người dùng quay lại payment.html
 */
async function handleVNPayCallbackReturn() {
  const loading = document.getElementById('payment-loading');
  const panel = document.getElementById('payment-panel');
  const loadingDesc = document.getElementById('loading-desc');
  if (loadingDesc) loadingDesc.textContent = 'Đang xác thực kết quả giao dịch từ Cổng VNPay...';

  try {
    const callbackUrl = `/api/payments/vnpay-callback${window.location.search}`;
    const res = await apiFetch(callbackUrl);

    loading.style.display = 'none';
    panel.style.display = 'block';

    currentOrderId = res.orderId;
    document.getElementById('order-id-label').textContent = res.orderId || '-';
    document.getElementById('payment-amount').textContent = formatPrice(res.amount);
    document.getElementById('payment-method-badge').innerHTML = '<span class="ribbon ribbon-lavender">Cổng thanh toán VNPay</span>';

    // Ẩn action box ban đầu
    const actionWrap = document.getElementById('payment-action-wrap');
    if (actionWrap) actionWrap.style.display = 'none';

    if (res.code === '00') {
      // Giao dịch thành công
      showPaymentSuccess({
        transactionId: res.transactionId,
        paidAt: res.paidAt || new Date()
      });
      showToast('Thanh toán VNPay thành công!', 'success');
    } else {
      // Giao dịch thất bại hoặc bị hủy
      showPaymentFailed(res.message);
    }

  } catch (err) {
    loading.style.display = 'none';
    panel.style.display = 'block';
    showPaymentFailed(err.message || 'Không thể xác thực giao dịch');
  }
}

/**
 * Tải thông tin đơn hàng chờ thanh toán
 */
async function loadOrderInfo(orderId) {
  const loading = document.getElementById('payment-loading');
  const panel = document.getElementById('payment-panel');

  try {
    const order = await apiFetch(`/api/orders/${orderId}`);
    loading.style.display = 'none';
    panel.style.display = 'block';

    document.getElementById('order-id-label').textContent = order.id;
    document.getElementById('payment-amount').textContent = formatPrice(order.totalAmount);

    const method = order.payment?.method || 'VNPAY';
    document.getElementById('payment-method-badge').innerHTML = `
      <span class="ribbon ribbon-lavender">${getPaymentMethodLabel(method)}</span>
    `;

    // Nếu đơn đã thanh toán trước đó
    if (order.status === 'PAID' || order.payment?.status === 'SUCCESS') {
      showPaymentSuccess(order.payment);
    }

  } catch (err) {
    loading.innerHTML = `<p style="color: var(--ribbon-coral);">Không thể tải thông tin đơn hàng: ${err.message}</p>`;
  }
}

function showPaymentSuccess(paymentData) {
  const actionWrap = document.getElementById('payment-action-wrap');
  if (actionWrap) actionWrap.style.display = 'none';

  const failedUi = document.getElementById('payment-failed-ui');
  if (failedUi) failedUi.style.display = 'none';

  const successUi = document.getElementById('payment-success-ui');
  if (successUi) successUi.style.display = 'block';

  document.getElementById('transaction-id-text').textContent = paymentData?.transactionId || ('VNPAY-' + Date.now());
  document.getElementById('payment-time-text').textContent = formatDateTime(paymentData?.paidAt || new Date());
}

function showPaymentFailed(message) {
  const actionWrap = document.getElementById('payment-action-wrap');
  if (actionWrap) actionWrap.style.display = 'none';

  const successUi = document.getElementById('payment-success-ui');
  if (successUi) successUi.style.display = 'none';

  const failedUi = document.getElementById('payment-failed-ui');
  if (failedUi) failedUi.style.display = 'block';

  document.getElementById('failed-reason-text').textContent = message || 'Giao dịch bị hủy hoặc không thành công.';
}

function bindEvents() {
  const btnVnpay = document.getElementById('btn-vnpay-pay');
  const btnRetry = document.getElementById('btn-retry-pay');

  // Nút thanh toán qua Cổng VNPay
  if (btnVnpay) {
    btnVnpay.addEventListener('click', async () => {
      btnVnpay.disabled = true;
      btnVnpay.textContent = 'Đang chuyển hướng tới VNPay...';

      try {
        const res = await apiFetch(`/api/payments/${currentOrderId}/vnpay-url`, {
          method: 'POST'
        });

        if (res.paymentUrl) {
          window.location.href = res.paymentUrl;
        } else {
          throw new Error('Không nhận được URL thanh toán từ máy chủ.');
        }

      } catch (err) {
        btnVnpay.disabled = false;
        btnVnpay.textContent = 'Thanh toán qua Cổng VNPay →';
        showToast(err.message || 'Lỗi khởi tạo cổng thanh toán VNPay', 'error');
      }
    });
  }

  // Nút thử lại khi giao dịch bị hủy
  if (btnRetry) {
    btnRetry.addEventListener('click', () => {
      if (currentOrderId) {
        window.location.href = `/payment.html?orderId=${currentOrderId}`;
      } else {
        window.location.href = '/my-orders.html';
      }
    });
  }
}