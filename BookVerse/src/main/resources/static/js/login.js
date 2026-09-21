document.addEventListener('DOMContentLoaded', () => {
  // If already logged in, redirect home
  if (isLoggedIn()) {
    window.location.href = '/index.html';
    return;
  }

  const form = document.getElementById('login-form');
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

    const userName = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!userName || !password) {
      generalError.textContent = 'Vui lòng nhập đầy đủ thông tin đăng nhập.';
      generalError.classList.add('show');
      return;
    }

    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Đang xác thực...';

    try {
      const res = await apiFetch('/api/auth/login', {
        method: 'POST',
        body: { userName, password }
      });

      // Save token, role, username and fullName
      setAuth(res.token, res.role, res.userName || userName, res.fullName);
      showToast('Đăng nhập thành công! Đang chuyển hướng...', 'success');

      // Check redirect param
      const params = new URLSearchParams(window.location.search);
      const redirectUrl = params.get('redirect') || (res.role === 'ROLE_ADMIN' ? '/admin/dashboard.html' : '/index.html');
      
      setTimeout(() => {
        window.location.href = redirectUrl;
      }, 500);

    } catch (err) {
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Đăng nhập';

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
        generalError.textContent = err.message || 'Đăng nhập thất bại. Vui lòng thử lại.';
        generalError.classList.add('show');
      }
    }
  });
});