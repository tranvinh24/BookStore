let currentCart = null;

document.addEventListener('DOMContentLoaded', async () => {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để tiến hành đặt hàng!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
    }, 800);
    return;
  }

  await loadCheckoutCart();
  bindEvents();
});

async function loadCheckoutCart() {
  try {
    currentCart = await apiFetch('/api/cart');

    if (!currentCart || !currentCart.items || currentCart.items.length === 0) {
      showToast('Giỏ hàng của bạn đang trống!', 'warning');
      setTimeout(() => {
        window.location.href = '/cart.html';
      }, 1000);
      return;
    }

    renderCheckoutSummary(currentCart);

  } catch (err) {
    showToast(`Không thể tải thông tin giỏ hàng: ${err.message}`, 'error');
  }
}

function renderCheckoutSummary(cart) {
  const subtotalEl = document.getElementById('chk-subtotal');
  const totalEl = document.getElementById('chk-total');

  if (subtotalEl) subtotalEl.textContent = formatPrice(cart.totalAmount);
  if (totalEl) totalEl.textContent = formatPrice(cart.totalAmount);

  const preview = document.getElementById('checkout-items-preview');
  if (!preview) return;

  preview.innerHTML = '';

  cart.items.forEach(item => {
    const row = document.createElement('div');
    row.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px dashed var(--border);';
    row.innerHTML = `
      <div style="flex: 1; padding-right: 0.5rem;">
        <div style="font-weight: 600; color: var(--text-main); line-height: 1.3;">${item.title}</div>
        <div style="color: var(--text-secondary); font-size: 0.8rem; margin-top: 2px;">
          Số lượng: <strong>${item.quantity}</strong> × ${formatPrice(item.price)}
        </div>
      </div>
      <div class="font-number" style="font-weight: 700; color: var(--text-main); white-space: nowrap;">
        ${formatPrice(item.subtotal)}
      </div>
    `;
    preview.appendChild(row);
  });
}

function bindEvents() {
  // Payment option styling & selection
  document.querySelectorAll('.payment-option').forEach(opt => {
    opt.addEventListener('click', () => {
      document.querySelectorAll('.payment-option').forEach(o => o.classList.remove('active'));
      opt.classList.add('active');
      const radio = opt.querySelector('input[type="radio"]');
      if (radio) radio.checked = true;
    });
  });

  // Handle Checkout Form Submit
  const form = document.getElementById('checkout-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Clear previous errors
    document.querySelectorAll('.field-error').forEach(el => {
      el.classList.remove('show');
      el.textContent = '';
    });
    document.querySelectorAll('.field-input').forEach(el => el.classList.remove('has-error'));

    const name = document.getElementById('recipient-name').value.trim();
    const phone = document.getElementById('recipient-phone').value.trim();
    const address = document.getElementById('shipping-address').value.trim();
    const note = document.getElementById('order-note')?.value.trim() || '';
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'COD';

    let hasError = false;

    if (!name) {
      showFieldError('recipient-name', 'Vui lòng nhập họ tên người nhận');
      hasError = true;
    }

    if (!phone) {
      showFieldError('recipient-phone', 'Vui lòng nhập số điện thoại người nhận');
      hasError = true;
    }

    if (!address) {
      showFieldError('shipping-address', 'Vui lòng nhập địa chỉ nhận hàng chi tiết');
      hasError = true;
    }

    if (hasError) return;

    const fullShippingAddress = `${name} | SĐT: ${phone} | Đ/c: ${address}${note ? ' (Ghi chú: ' + note + ')' : ''}`;

    const btnSubmit = document.getElementById('btn-submit-order');
    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Đang xử lý đặt hàng...';

    try {
      const order = await apiFetch('/api/orders/checkout', {
        method: 'POST',
        body: {
          shippingAddress: fullShippingAddress,
          paymentMethod: paymentMethod
        }
      });

      showToast('Đặt hàng thành công!', 'success');
      updateBadges();

      setTimeout(() => {
        if (paymentMethod === 'COD') {
          window.location.href = '/my-orders.html';
        } else {
          window.location.href = `/payment.html?orderId=${order.id}`;
        }
      }, 600);

    } catch (err) {
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Xác nhận đặt hàng';

      if (err.validationErrors) {
        Object.keys(err.validationErrors).forEach(field => {
          showFieldError(field, err.validationErrors[field]);
        });
      } else {
        showToast(err.message || 'Không thể tạo đơn hàng. Vui lòng thử lại.', 'error');
      }
    }
  });
}

function showFieldError(fieldId, msg) {
  const errEl = document.getElementById(`err-${fieldId}`) || document.getElementById(`${fieldId}-error`);
  const inputEl = document.getElementById(fieldId);
  if (errEl) {
    errEl.textContent = msg;
    errEl.classList.add('show');
  }
  if (inputEl) {
    inputEl.classList.add('has-error');
    inputEl.focus();
  }
}