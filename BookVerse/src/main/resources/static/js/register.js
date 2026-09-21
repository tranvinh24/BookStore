document.addEventListener('DOMContentLoaded', () => {
  if (isLoggedIn()) {
    window.location.href = '/index.html';
    return;
  }

  const form = document.getElementById('register-form');
  const btnSubmit = document.getElementById('btn-submit');
  const generalError = document.getElementById('form-general-error');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Clear old errors
    generalError.classList.remove('show');
    generalError.textContent = '';
    document.querySelectorAll('.field-error').forEach(el => {
      el.classList.remove('show');
      el.textContent = '';
    });
    document.querySelectorAll('.field-input').forEach(el => el.classList.remove('has-error'));

    const userName = document.getElementById('userName').value.trim();
    const email = document.getElementById('email').value.trim();
    const sdt = document.getElementById('sdt').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
      const errEl = document.getElementById('confirmPassword-error');
      errEl.textContent = 'Mật khẩu xác nhận không khớp.';
      errEl.classList.add('show');
      document.getElementById('confirmPassword').classList.add('has-error');
      return;
    }

    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Đang xử lý...';

    try {
      const payload = { userName, email, password };
      if (sdt) payload.sdt = sdt;

      const res = await apiFetch('/api/auth/register', {
        method: 'POST',
        body: payload
      });

      // Save token, role, username and fullName
      setAuth(res.token, res.role, res.userName || userName, res.fullName);
      showToast('Đăng ký tài khoản thành công!', 'success');

      setTimeout(() => {
        window.location.href = '/index.html';
      }, 600);

    } catch (err) {
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Đăng ký tài khoản';

      if (err.validationErrors) {
        Object.keys(err.validationErrors).forEach(field => {
          const errEl = document.getElementById(`${field}-error`);
          const inputEl = document.getElementById(field);
          if (errEl) {
            errEl.textContent = err.validationErrors[field];
            errEl.classList.add('show');
          }
          if (inputEl) inputEl.classList.add('has-error');
        });
      } else {
        generalError.textContent = err.message || 'Đăng ký không thành công. Vui lòng kiểm tra lại.';
        generalError.classList.add('show');
      }
    }
  });
});