/* profile.js — Hồ sơ cá nhân BookVerse */

let userProfile = null;
let currentTab = 'info';

// ─── Bootstrap ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  if (!isLoggedIn()) {
    showPageToast('Vui lòng đăng nhập để truy cập hồ sơ!', 'info');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
    }, 900);
    return;
  }

  await loadProfile();
  bindFormEvents();
  bindAvatarUpload();
});

// ─── Tab switching ────────────────────────────────────────────
function switchTab(tab) {
  currentTab = tab;
  document.querySelectorAll('.panel-section').forEach(el => el.classList.remove('active'));
  document.getElementById(`tab-${tab}`).classList.add('active');
  document.querySelectorAll('.sidebar-nav-btn').forEach(btn => btn.classList.remove('active'));
  document.getElementById(`btn-nav-${tab}`).classList.add('active');
}

// ─── Load profile ─────────────────────────────────────────────
async function loadProfile() {
  try {
    userProfile = await apiFetch('/api/profile');
    renderAll(userProfile);
    updateNavbarAvatar(userProfile); // đồng bộ avatar trên navbar
  } catch (err) {
    showPageToast('Không thể tải hồ sơ: ' + err.message, 'error');
  }
}

// ─── Render ───────────────────────────────────────────────────
function renderAll(user) {
  document.getElementById('sidebar-username').textContent = user.userName || '–';
  document.getElementById('sidebar-fullname').textContent = user.fullName || 'Chưa cập nhật tên';
  document.getElementById('sidebar-id').textContent = user.id ? user.id.substring(0, 8) + '...' : '–';
  document.getElementById('sidebar-created').textContent = formatDate(user.createdAt);

  const roleBadge = document.getElementById('sidebar-role-badge');
  roleBadge.innerHTML = user.role === 'ROLE_ADMIN'
    ? '<span class="ribbon ribbon-lavender">Quản trị viên</span>'
    : '<span class="ribbon ribbon-sage">Độc giả thân thiết</span>';

  renderAvatar(user.avatarPath, user.fullName || user.userName);

  document.getElementById('field-fullname').value = user.fullName || '';
  document.getElementById('field-email').value    = user.email || '';
  document.getElementById('field-sdt').value      = user.sdt || '';
  document.getElementById('field-dob').value      = user.dateOfBirth || '';
}

// ─── Avatar render ────────────────────────────────────────────
function renderAvatar(avatarPath, displayName) {
  const initial = (displayName || 'U').charAt(0).toUpperCase();

  const sidebarInitial = document.getElementById('sidebar-avatar-initial');
  const sidebarImg     = document.getElementById('sidebar-avatar-img');
  const infoInitial    = document.getElementById('info-avatar-initial');
  const infoImg        = document.getElementById('info-avatar-img');

  const fallback = (imgEl, initialEl) => {
    imgEl.style.display = 'none';
    initialEl.textContent = initial;
    initialEl.style.display = 'flex';
  };

  // Chỉ dùng avatarPath nếu là URL tĩnh hợp lệ (bắt đầu bằng '/')
  // Path cũ dạng "2026/09/..." trong DB sẽ bị bỏ qua
  const validPath = avatarPath && avatarPath.startsWith('/');

  if (validPath) {
    const url = avatarPath + '?t=' + Date.now();

    sidebarImg.onerror = () => fallback(sidebarImg, sidebarInitial);
    sidebarImg.src = url;
    sidebarImg.style.display = 'block';
    sidebarInitial.style.display = 'none';

    infoImg.onerror = () => fallback(infoImg, infoInitial);
    infoImg.src = url;
    infoImg.style.display = 'block';
    infoInitial.style.display = 'none';
  } else {
    sidebarInitial.textContent = initial;
    sidebarInitial.style.display = 'flex';
    sidebarImg.style.display = 'none';
    sidebarImg.src = '';

    infoInitial.textContent = initial;
    infoInitial.style.display = 'flex';
    infoImg.style.display = 'none';
    infoImg.src = '';
  }
}

/** Cập nhật avatar trên thanh navbar sau khi upload */
function updateNavbarAvatar(user) {
  const circle = document.querySelector('#user-menu-btn .action-icon-circle');
  if (!circle) return;

  const initial = (user.userName || 'U').charAt(0).toUpperCase();
  const validPath = user.avatarPath && user.avatarPath.startsWith('/');

  if (validPath) {
    const url = user.avatarPath + '?t=' + Date.now();
    const img = document.createElement('img');
    img.src = url;
    img.alt = 'avatar';
    img.style.cssText = 'width:100%;height:100%;border-radius:50%;object-fit:cover;';
    img.onerror = () => { circle.textContent = initial; };
    circle.innerHTML = '';
    circle.appendChild(img);
  } else {
    circle.textContent = initial;
  }
}

// ─── Avatar upload ────────────────────────────────────────────
function bindAvatarUpload() {
  document.getElementById('avatar-file-input').addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      showPageToast('Ảnh quá lớn, tối đa 5 MB.', 'error');
      return;
    }

    // Preview tức thì bằng base64 (không chờ upload xong)
    const reader = new FileReader();
    reader.onload = (ev) => {
      const infoImg = document.getElementById('info-avatar-img');
      const sideImg = document.getElementById('sidebar-avatar-img');
      infoImg.src = ev.target.result;
      infoImg.style.display = 'block';
      document.getElementById('info-avatar-initial').style.display = 'none';
      sideImg.src = ev.target.result;
      sideImg.style.display = 'block';
      document.getElementById('sidebar-avatar-initial').style.display = 'none';
    };
    reader.readAsDataURL(file);

    // Upload lên server — apiFetch() tự đính token bookverse_token
    try {
      const formData = new FormData();
      formData.append('file', file);

      const updated = await apiFetch('/api/profile/avatar', {
        method: 'POST',
        body: formData
      });

      userProfile = updated;
      // Thay base64 preview bằng URL tĩnh thực từ server
      renderAvatar(updated.avatarPath, updated.fullName || updated.userName);
      // Cập nhật avatar trên navbar
      updateNavbarAvatar(updated);

      showPageToast('Cập nhật ảnh đại diện thành công! 🎉', 'success');
    } catch (err) {
      showPageToast(err.message, 'error');
    }
  });
}

// ─── Info form ────────────────────────────────────────────────
function bindFormEvents() {
  document.getElementById('form-info').addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors('info');

    const payload = {};
    const fullName = document.getElementById('field-fullname').value.trim();
    const email    = document.getElementById('field-email').value.trim();
    const sdt      = document.getElementById('field-sdt').value.trim();
    const dob      = document.getElementById('field-dob').value;

    if (fullName !== (userProfile.fullName || '')) payload.fullName = fullName;
    if (email    !== (userProfile.email    || '')) payload.email    = email;
    if (sdt      !== (userProfile.sdt      || '')) payload.sdt      = sdt;
    if (dob      !== (userProfile.dateOfBirth || '')) payload.dateOfBirth = dob || null;

    if (Object.keys(payload).length === 0) {
      showPageToast('Không có thay đổi nào cần lưu.', 'info');
      return;
    }

    const btn = document.getElementById('btn-save-info');
    btn.disabled = true;
    btn.textContent = 'Đang lưu...';

    try {
      const updated = await apiFetch('/api/profile', { method: 'PUT', body: payload });
      userProfile = updated;
      renderAll(updated);

      // Cập nhật fullName vào localStorage và navbar
      if (updated.fullName) {
        localStorage.setItem('bookverse_fullname', updated.fullName);
      }
      const navNameEl = document.getElementById('nav-user-display-name');
      if (navNameEl) {
        navNameEl.textContent = (updated.fullName || updated.userName) + ' ▾';
      }

      showPageToast('Cập nhật hồ sơ thành công!', 'success');
    } catch (err) {
      handleFormError(err, 'info');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Lưu thay đổi';
    }
  });

  // Đổi mật khẩu
  document.getElementById('form-password').addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors('pw');

    const newPw     = document.getElementById('field-new-password').value;
    const confirmPw = document.getElementById('field-confirm-password').value;
    const currentPw = document.getElementById('field-current-password').value;

    if (newPw !== confirmPw) {
      const errEl = document.getElementById('confirm-error');
      errEl.textContent = 'Mật khẩu mới và xác nhận không khớp.';
      errEl.classList.add('show');
      return;
    }

    const btn = document.getElementById('btn-save-password');
    btn.disabled = true;
    btn.textContent = 'Đang xử lý...';

    try {
      await apiFetch('/api/profile/change-password', {
        method: 'POST',
        body: { currentPassword: currentPw, newPassword: newPw, confirmPassword: confirmPw }
      });
      showPageToast('Đổi mật khẩu thành công!', 'success');
      document.getElementById('field-new-password').value     = '';
      document.getElementById('field-confirm-password').value = '';
      document.getElementById('field-current-password').value = '';
      document.getElementById('strength-bar').style.display   = 'none';
      document.getElementById('strength-label').textContent   = '';
    } catch (err) {
      handleFormError(err, 'pw');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Đổi mật khẩu';
    }
  });
}

// ─── Password strength meter ──────────────────────────────────
function checkPasswordStrength(value) {
  const bar   = document.getElementById('strength-bar');
  const label = document.getElementById('strength-label');

  if (!value) {
    bar.style.display = 'none';
    label.textContent = '';
    return;
  }

  bar.style.display = 'block';
  let score = 0;
  if (value.length >= 8) score++;
  if (/[A-Z]/.test(value)) score++;
  if (/[0-9]/.test(value)) score++;
  if (/[^A-Za-z0-9]/.test(value)) score++;

  bar.className = 'password-strength';
  if (score <= 1) {
    bar.classList.add('strength-weak');
    label.textContent = 'Yếu — thêm chữ hoa, số hoặc ký tự đặc biệt';
    label.style.color = '#ef4444';
  } else if (score <= 2) {
    bar.classList.add('strength-medium');
    label.textContent = 'Trung bình';
    label.style.color = '#d97706';
  } else {
    bar.classList.add('strength-strong');
    label.textContent = 'Mạnh 💪';
    label.style.color = '#15803d';
  }
}

// ─── Error helpers ────────────────────────────────────────────
function clearErrors(prefix) {
  const id = prefix === 'pw' ? 'pw-general-error' : 'info-general-error';
  const el = document.getElementById(id);
  if (el) { el.textContent = ''; el.classList.remove('show'); }
  document.querySelectorAll('.field-error').forEach(e => {
    e.textContent = '';
    e.classList.remove('show');
  });
}

function handleFormError(err, prefix) {
  const id = prefix === 'pw' ? 'pw-general-error' : 'info-general-error';
  const general = document.getElementById(id);
  if (err.validationErrors) {
    Object.keys(err.validationErrors).forEach(field => {
      const el = document.getElementById(`${field}-error`);
      if (el) { el.textContent = err.validationErrors[field]; el.classList.add('show'); }
    });
  }
  if (general) {
    general.textContent = err.message || 'Có lỗi xảy ra, vui lòng thử lại.';
    general.classList.add('show');
  }
}

// ─── Toast ────────────────────────────────────────────────────
function showPageToast(msg, type = 'info') {
  const toast = document.getElementById('profile-page-toast');
  if (!toast) return;
  toast.textContent = msg;
  toast.className = `profile-toast ${type} show`;
  clearTimeout(toast._timer);
  toast._timer = setTimeout(() => toast.classList.remove('show'), 3500);
}

function formatDate(dateStr) {
  if (!dateStr) return '–';
  try {
    return new Date(dateStr).toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' });
  } catch { return dateStr; }
}