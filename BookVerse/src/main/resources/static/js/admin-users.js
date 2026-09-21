document.addEventListener('DOMContentLoaded', async () => {
  if (!isAdmin()) {
    showToast('Khu vực dành riêng cho Quản trị viên!', 'error');
    setTimeout(() => window.location.href = '/index.html', 800);
    return;
  }

  await loadUsersList();
  bindEvents();
});

async function loadUsersList() {
  const tbody = document.getElementById('users-table-body');
  tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;">Đang tải danh sách...</td></tr>';

  try {
    const users = await apiFetch('/api/admin/users');

    if (!users || users.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;">Chưa có tài khoản nào.</td></tr>';
      return;
    }

    tbody.innerHTML = '';
    users.forEach(u => {
      const isCurrentAdmin = u.userName === getUsername();
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>
          <div style="display: flex; align-items: center; gap: 0.6rem;">
            <div class="user-avatar" style="width: 32px; height: 32px; font-size: 0.9rem;">
              ${(u.userName || 'U').charAt(0).toUpperCase()}
            </div>
            <div>
              <strong style="color: var(--ink);">${u.userName}</strong>
              ${isCurrentAdmin ? '<span class="ribbon ribbon-marigold" style="margin-left: 4px; font-size: 0.7rem;">Bạn</span>' : ''}
            </div>
          </div>
        </td>
        <td>${u.email || 'Chưa cập nhật'}</td>
        <td class="font-number">${u.sdt || '—'}</td>
        <td>
          ${u.role === 'ROLE_ADMIN' || u.role === 'ADMIN' 
            ? '<span class="ribbon ribbon-lavender">ADMIN</span>' 
            : '<span class="ribbon ribbon-sage">USER</span>'}
        </td>
        <td style="font-size: 0.85rem; color: var(--ink-muted);">${formatDate(u.createdAt)}</td>
        <td style="text-align: right;">
          <div style="display: flex; justify-content: flex-end; gap: 0.4rem;">
            <button class="btn btn-outline btn-sm btn-edit-user" data-user='${JSON.stringify(u).replace(/'/g, "&apos;")}'>
              Sửa
            </button>
            <button class="btn btn-outline btn-sm btn-delete-user" data-id="${u.id}" ${isCurrentAdmin ? 'disabled title="Không thể xóa chính mình"' : ''} style="color: var(--ribbon-coral); border-color: var(--border);">
              Xóa
            </button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });

    // Bind edit buttons
    document.querySelectorAll('.btn-edit-user').forEach(btn => {
      btn.addEventListener('click', () => {
        const u = JSON.parse(btn.dataset.user);
        openEditUserModal(u);
      });
    });

    // Bind delete buttons
    document.querySelectorAll('.btn-delete-user:not([disabled])').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc muốn xóa vĩnh viễn tài khoản người dùng này?')) return;
        try {
          await apiFetch(`/api/admin/users/${btn.dataset.id}`, { method: 'DELETE' });
          showToast('Đã xóa tài khoản thành công!', 'success');
          loadUsersList();
        } catch (err) {
          showToast(err.message || 'Không thể xóa tài khoản', 'error');
        }
      });
    });

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--ribbon-coral);">${err.message}</td></tr>`;
  }
}

function openEditUserModal(user) {
  document.getElementById('edit-user-id').value = user.id;
  document.getElementById('edit-username').value = user.userName;
  document.getElementById('edit-email').value = user.email || '';
  document.getElementById('edit-sdt').value = user.sdt || '';
  
  const roleSelect = document.getElementById('edit-role');
  roleSelect.value = (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN') ? 'ROLE_ADMIN' : 'ROLE_USER';

  document.getElementById('user-modal-error').classList.remove('show');
  document.getElementById('user-modal').classList.add('show');
}

function bindEvents() {
  document.getElementById('btn-close-user-modal').addEventListener('click', () => {
    document.getElementById('user-modal').classList.remove('show');
  });
  document.getElementById('btn-cancel-user-modal').addEventListener('click', () => {
    document.getElementById('user-modal').classList.remove('show');
  });

  // Submit User Edit
  document.getElementById('user-edit-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const userId = document.getElementById('edit-user-id').value;
    const email = document.getElementById('edit-email').value.trim();
    const sdt = document.getElementById('edit-sdt').value.trim();
    const roleVal = document.getElementById('edit-role').value;
    const role = roleVal.replace('ROLE_', ''); // backend enum is USER or ADMIN

    const errEl = document.getElementById('user-modal-error');
    errEl.classList.remove('show');

    const btnSave = document.getElementById('btn-save-user');
    btnSave.disabled = true;
    btnSave.textContent = 'Đang lưu...';

    try {
      await apiFetch(`/api/admin/users/${userId}`, {
        method: 'PUT',
        body: {
          email,
          sdt: sdt || undefined,
          role
        }
      });

      showToast('Cập nhật tài khoản người dùng thành công!', 'success');
      document.getElementById('user-modal').classList.remove('show');
      loadUsersList();

    } catch (err) {
      errEl.textContent = err.message || 'Cập nhật tài khoản thất bại.';
      errEl.classList.add('show');
    } finally {
      btnSave.disabled = false;
      btnSave.textContent = 'Lưu thay đổi';
    }
  });
}