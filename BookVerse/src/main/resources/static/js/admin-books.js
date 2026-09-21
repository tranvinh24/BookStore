let currentPage = 0;
let currentSearch = '';
let selectedFile = null;
let selectedImportFile = null;
let currentEditingStockBookId = null;

document.addEventListener('DOMContentLoaded', async () => {
  if (!isAdmin()) {
    showToast('Khu vực dành riêng cho Quản trị viên!', 'error');
    setTimeout(() => window.location.href = '/index.html', 800);
    return;
  }

  await loadBooksList();
  bindEvents();
});

async function loadBooksList() {
  const tbody = document.getElementById('books-table-body');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">Đang tải danh sách...</td></tr>';

  try {
    const endpoint = currentSearch 
      ? `/api/books/search?q=${encodeURIComponent(currentSearch)}&page=${currentPage}&size=10`
      : `/api/books?page=${currentPage}&size=10`;

    const data = await apiFetch(endpoint);

    if (!data || !data.content || data.content.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">Không có sách nào.</td></tr>';
      document.getElementById('admin-pagination').style.display = 'none';
      return;
    }

    renderTableRows(data.content);
    renderAdminPagination(data);

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--ribbon-coral);">${err.message}</td></tr>`;
  }
}

function renderTableRows(books) {
  const tbody = document.getElementById('books-table-body');
  tbody.innerHTML = '';
  const ts = Date.now(); // cache-busting: ảnh mới luôn được tải lại sau mọi lần gọi render

  books.forEach(b => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div style="width: 50px; height: 65px; background: var(--paper-warm); border-radius: var(--radius-sm); overflow: hidden;">
          <img src="/api/books/${b.id}/cover?size=thumbnail&v=${ts}" alt="${b.title}" style="width: 100%; height: 100%; object-fit: cover;"
               onerror="this.parentElement.innerHTML='<div style=\'height:100%;display:flex;align-items:center;justify-content:center;\'></div>'" />
        </div>
      </td>
      <td>
        <div style="font-weight: 600; color: var(--ink);">${b.title}</div>
        <div style="font-size: 0.8rem; color: var(--ink-muted);">${b.author || 'Chưa rõ'} — Năm ${b.year || 'N/A'}</div>
      </td>
      <td><span class="ribbon ribbon-lavender">${b.category || 'Chung'}</span></td>
      <td class="font-number" style="font-weight: 700;">${formatPrice(b.price)}</td>
      <td>
        <div style="display: flex; align-items: center; gap: 0.4rem;">
          <span class="font-number" style="font-weight: 700; ${b.stock <= 5 ? 'color: var(--ribbon-coral);' : ''}">${b.stock}</span>
          <button class="btn btn-outline btn-sm btn-quick-stock" data-id="${b.id}" data-title="${b.title}" data-stock="${b.stock}" title="Chỉnh kho" style="padding: 0.2rem 0.4rem; font-size: 0.75rem;">
            
          </button>
        </div>
      </td>
      <td class="font-number" style="color: #E28725; font-weight: 600;">★ ${b.rating ? b.rating.toFixed(1) : '5.0'}</td>
      <td style="text-align: right;">
        <div style="display: flex; justify-content: flex-end; gap: 0.4rem;">
          <button class="btn btn-outline btn-sm btn-edit-book" data-book='${JSON.stringify(b).replace(/'/g, "&apos;")}'>
            Sửa
          </button>
          <button class="btn btn-outline btn-sm btn-delete-book" data-id="${b.id}" style="color: var(--ribbon-coral); border-color: var(--border);">
            Xóa
          </button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });

  // Bind actions
  document.querySelectorAll('.btn-quick-stock').forEach(btn => {
    btn.addEventListener('click', () => {
      currentEditingStockBookId = btn.dataset.id;
      document.getElementById('stock-modal-title').textContent = btn.dataset.title;
      document.getElementById('quick-stock-input').value = btn.dataset.stock;
      document.getElementById('stock-modal').classList.add('show');
    });
  });

  document.querySelectorAll('.btn-edit-book').forEach(btn => {
    btn.addEventListener('click', () => {
      const book = JSON.parse(btn.dataset.book);
      openEditModal(book);
    });
  });

  document.querySelectorAll('.btn-delete-book').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (!confirm('Bạn có chắc chắn muốn xóa cuốn sách này?')) return;
      try {
        await apiFetch(`/api/books/${btn.dataset.id}`, { method: 'DELETE' });
        showToast('Đã xóa sách thành công!', 'success');
        loadBooksList();
      } catch (err) {
        showToast(err.message || 'Không thể xóa sách', 'error');
      }
    });
  });
}

function renderAdminPagination(data) {
  const wrap = document.getElementById('admin-pagination');
  if (data.totalPages <= 1) {
    wrap.style.display = 'none';
    return;
  }
  wrap.style.display = 'flex';
  wrap.innerHTML = '';

  const prev = document.createElement('button');
  prev.className = 'page-btn';
  prev.innerHTML = '‹';
  prev.disabled = data.page === 0;
  prev.onclick = () => { currentPage--; loadBooksList(); };
  wrap.appendChild(prev);

  for (let i = 0; i < data.totalPages; i++) {
    const btn = document.createElement('button');
    btn.className = `page-btn ${data.page === i ? 'active' : ''}`;
    btn.textContent = i + 1;
    btn.onclick = () => { currentPage = i; loadBooksList(); };
    wrap.appendChild(btn);
  }

  const next = document.createElement('button');
  next.className = 'page-btn';
  next.innerHTML = '›';
  next.disabled = data.last || data.page >= data.totalPages - 1;
  next.onclick = () => { currentPage++; loadBooksList(); };
  wrap.appendChild(next);
}

function openCreateModal() {
  document.getElementById('modal-title').textContent = 'Thêm sách mới';
  document.getElementById('form-book-id').value = '';
  document.getElementById('book-form').reset();
  document.getElementById('cover-preview-box').style.display = 'none';
  selectedFile = null;
  clearModalErrors();
  document.getElementById('book-modal').classList.add('show');
}

function openEditModal(book) {
  document.getElementById('modal-title').textContent = `Chỉnh sửa: ${book.title}`;
  document.getElementById('form-book-id').value = book.id;
  document.getElementById('form-title').value = book.title || '';
  document.getElementById('form-author').value = book.author || '';
  document.getElementById('form-category').value = book.category || '';
  document.getElementById('form-price').value = book.price || 0;
  document.getElementById('form-stock').value = book.stock || 0;
  document.getElementById('form-year').value = book.year || 2024;
  document.getElementById('form-isbn').value = book.isbn || '';
  document.getElementById('form-rating').value = book.rating || 5.0;
  document.getElementById('form-description').value = book.description || '';

  const previewBox = document.getElementById('cover-preview-box');
  const previewImg = document.getElementById('cover-preview-img');
  // Thêm ?v=timestamp để tránh hiển thị ảnh cũ từ cache khi mở modal sửa
  previewImg.src = `/api/books/${book.id}/cover?size=thumbnail&v=${Date.now()}`;
  previewBox.style.display = 'block';

  selectedFile = null;
  clearModalErrors();
  document.getElementById('book-modal').classList.add('show');
}

function clearModalErrors() {
  document.getElementById('modal-general-error').classList.remove('show');
  document.querySelectorAll('#book-modal .field-error').forEach(e => {
    e.classList.remove('show');
    e.textContent = '';
  });
  document.querySelectorAll('#book-modal .field-input').forEach(e => e.classList.remove('has-error'));
}

function bindEvents() {
  // Search
  document.getElementById('btn-search-books').addEventListener('click', () => {
    currentSearch = document.getElementById('admin-book-search').value.trim();
    currentPage = 0;
    loadBooksList();
  });

  // Modal open/close
  document.getElementById('btn-open-create-modal').addEventListener('click', openCreateModal);
  document.getElementById('btn-close-modal').addEventListener('click', () => {
    document.getElementById('book-modal').classList.remove('show');
  });
  document.getElementById('btn-cancel-modal').addEventListener('click', () => {
    document.getElementById('book-modal').classList.remove('show');
  });

  // Import Modal open/close
  const btnOpenImport = document.getElementById('btn-open-import-modal');
  if (btnOpenImport) {
    btnOpenImport.addEventListener('click', openImportModal);
  }
  const btnCloseImport = document.getElementById('btn-close-import-modal');
  if (btnCloseImport) {
    btnCloseImport.addEventListener('click', () => {
      document.getElementById('import-modal').classList.remove('show');
    });
  }
  const btnCancelImport = document.getElementById('btn-cancel-import');
  if (btnCancelImport) {
    btnCancelImport.addEventListener('click', () => {
      document.getElementById('import-modal').classList.remove('show');
    });
  }

  // Import Drag & Drop & File Selection
  const importDropZone = document.getElementById('import-drop-zone');
  const importFileInput = document.getElementById('import-file-input');

  if (importDropZone && importFileInput) {
    importDropZone.addEventListener('click', () => importFileInput.click());

    ['dragenter', 'dragover'].forEach(name => {
      importDropZone.addEventListener(name, (e) => {
        e.preventDefault();
        importDropZone.classList.add('dragover');
      });
    });

    ['dragleave', 'drop'].forEach(name => {
      importDropZone.addEventListener(name, (e) => {
        e.preventDefault();
        importDropZone.classList.remove('dragover');
      });
    });

    importDropZone.addEventListener('drop', (e) => {
      const files = e.dataTransfer.files;
      if (files.length > 0) handleImportFileSelection(files[0]);
    });

    importFileInput.addEventListener('change', (e) => {
      if (e.target.files.length > 0) handleImportFileSelection(e.target.files[0]);
    });
  }

  // Submit Import Form
  const importForm = document.getElementById('import-form');
  if (importForm) {
    importForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!selectedImportFile) {
        showImportError('Vui lòng chọn file Excel hoặc CSV để nhập.');
        return;
      }

      const btnSubmit = document.getElementById('btn-submit-import');
      btnSubmit.disabled = true;
      btnSubmit.textContent = '⏳ Đang xử lý dữ liệu...';
      clearImportErrors();

      try {
        const formData = new FormData();
        formData.append('file', selectedImportFile);

        const res = await apiFetch('/api/books/import', {
          method: 'POST',
          body: formData
        });

        // Hiển thị kết quả chi tiết
        document.getElementById('import-result-panel').style.display = 'block';
        document.getElementById('res-total-rows').textContent = res.totalRows || 0;
        document.getElementById('res-success-rows').textContent = res.successCount || 0;
        document.getElementById('res-failed-rows').textContent = res.failedCount || 0;

        const errorsContainer = document.getElementById('import-errors-container');
        const errorsBody = document.getElementById('import-errors-body');
        errorsBody.innerHTML = '';

        if (res.errors && res.errors.length > 0) {
          errorsContainer.style.display = 'block';
          res.errors.forEach(err => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
              <td class="font-number" style="font-weight: 600;">${err.rowNumber}</td>
              <td style="font-weight: 500;">${err.title || '(Không có)'}</td>
              <td style="color: var(--ribbon-coral);">${err.errorMessage}</td>
            `;
            errorsBody.appendChild(tr);
          });
        } else {
          errorsContainer.style.display = 'none';
        }

        if (res.successCount > 0) {
          showToast(`Nhập thành công ${res.successCount} cuốn sách!`, 'success');
          loadBooksList();
        } else {
          showToast('Không có dòng dữ liệu hợp lệ nào được nhập.', 'warning');
        }

      } catch (err) {
        showImportError(err.message || 'Lỗi khi tải file lên server');
      } finally {
        btnSubmit.disabled = false;
        btnSubmit.textContent = '🚀 Bắt đầu nhập sách';
      }
    });
  }

  // Stock modal close
  document.getElementById('btn-close-stock-modal').addEventListener('click', () => {
    document.getElementById('stock-modal').classList.remove('show');
  });
  document.getElementById('btn-cancel-stock').addEventListener('click', () => {
    document.getElementById('stock-modal').classList.remove('show');
  });

  // Save stock
  document.getElementById('btn-save-stock').addEventListener('click', async () => {
    const stockVal = parseInt(document.getElementById('quick-stock-input').value);
    if (isNaN(stockVal) || stockVal < 0) {
      showToast('Số lượng tồn kho phải >= 0', 'warning');
      return;
    }

    try {
      await apiFetch(`/api/admin/books/${currentEditingStockBookId}/stock`, {
        method: 'PATCH',
        body: { stock: stockVal }
      });
      showToast('Đã cập nhật số lượng tồn kho!', 'success');
      document.getElementById('stock-modal').classList.remove('show');
      loadBooksList();
    } catch (err) {
      showToast(err.message || 'Không thể cập nhật kho', 'error');
    }
  });

  // Drag & drop file upload
  const dropZone = document.getElementById('cover-drop-zone');
  const fileInput = document.getElementById('form-cover-file');

  dropZone.addEventListener('click', () => fileInput.click());

  ['dragenter', 'dragover'].forEach(name => {
    dropZone.addEventListener(name, (e) => {
      e.preventDefault();
      dropZone.classList.add('dragover');
    });
  });

  ['dragleave', 'drop'].forEach(name => {
    dropZone.addEventListener(name, (e) => {
      e.preventDefault();
      dropZone.classList.remove('dragover');
    });
  });

  dropZone.addEventListener('drop', (e) => {
    const files = e.dataTransfer.files;
    if (files.length > 0) handleFileSelection(files[0]);
  });

  fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) handleFileSelection(e.target.files[0]);
  });

  // Submit Book Form
  document.getElementById('book-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    clearModalErrors();

    const bookId = document.getElementById('form-book-id').value;
    const isEdit = !!bookId;

    const dataPayload = {
      title: document.getElementById('form-title').value.trim(),
      author: document.getElementById('form-author').value.trim(),
      category: document.getElementById('form-category').value.trim(),
      price: parseInt(document.getElementById('form-price').value) || 0,
      stock: parseInt(document.getElementById('form-stock').value) || 0,
      year: parseInt(document.getElementById('form-year').value) || 2024,
      rating: parseFloat(document.getElementById('form-rating').value) || 5.0,
      isbn: document.getElementById('form-isbn').value.trim() || undefined,
      description: document.getElementById('form-description').value.trim() || undefined
    };

    const btnSave = document.getElementById('btn-save-book');
    btnSave.disabled = true;
    btnSave.textContent = 'Đang lưu...';

    try {
      if (selectedFile) {
        // Multipart upload
        const formData = new FormData();
        formData.append('data', new Blob([JSON.stringify(dataPayload)], { type: 'application/json' }));
        formData.append('cover', selectedFile);

        const endpoint = isEdit ? `/api/books/${bookId}` : '/api/books';
        const method = isEdit ? 'PUT' : 'POST';

        await apiFetch(endpoint, {
          method,
          body: formData
        });
      } else {
        // Plain JSON
        const endpoint = isEdit ? `/api/books/${bookId}` : '/api/books';
        const method = isEdit ? 'PUT' : 'POST';

        await apiFetch(endpoint, {
          method,
          body: dataPayload
        });
      }

      showToast(isEdit ? 'Cập nhật sách thành công!' : 'Thêm sách mới thành công!', 'success');
      document.getElementById('book-modal').classList.remove('show');
      loadBooksList();

    } catch (err) {
      if (err.validationErrors) {
        Object.keys(err.validationErrors).forEach(field => {
          const errEl = document.getElementById(`${field}-error`);
          const inputEl = document.getElementById(`form-${field}`);
          if (errEl) {
            errEl.textContent = err.validationErrors[field];
            errEl.classList.add('show');
          }
          if (inputEl) inputEl.classList.add('has-error');
        });
      } else {
        const genErr = document.getElementById('modal-general-error');
        genErr.textContent = err.message || 'Không thể lưu sách. Vui lòng kiểm tra lại.';
        genErr.classList.add('show');
      }
    } finally {
      btnSave.disabled = false;
      btnSave.textContent = 'Lưu thông tin sách';
    }
  });
}

function handleFileSelection(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
  if (!allowed.includes(file.type)) {
    showToast('Định dạng ảnh không hỗ trợ! Vui lòng chọn file JPG, PNG hoặc WebP.', 'error');
    return;
  }

  // 20MB limit
  if (file.size > 20 * 1024 * 1024) {
    showToast('Dung lượng file vượt quá giới hạn 20MB!', 'error');
    return;
  }

  selectedFile = file;
  const previewBox = document.getElementById('cover-preview-box');
  const previewImg = document.getElementById('cover-preview-img');

  const reader = new FileReader();
  reader.onload = (e) => {
    previewImg.src = e.target.result;
    previewBox.style.display = 'block';
  };
  reader.readAsDataURL(file);
}

function openImportModal() {
  selectedImportFile = null;
  const fileInput = document.getElementById('import-file-input');
  if (fileInput) fileInput.value = '';

  const nameEl = document.getElementById('import-file-selected-name');
  if (nameEl) {
    nameEl.style.display = 'none';
    nameEl.textContent = '';
  }

  const resPanel = document.getElementById('import-result-panel');
  if (resPanel) resPanel.style.display = 'none';

  const btnSubmit = document.getElementById('btn-submit-import');
  if (btnSubmit) {
    btnSubmit.disabled = true;
    btnSubmit.textContent = '🚀 Bắt đầu nhập sách';
  }

  clearImportErrors();
  document.getElementById('import-modal').classList.add('show');
}

function handleImportFileSelection(file) {
  const ext = file.name.split('.').pop().toLowerCase();
  if (!['xlsx', 'xls', 'csv'].includes(ext)) {
    showImportError('Chỉ hỗ trợ file Excel (.xlsx, .xls) hoặc file CSV (.csv)');
    return;
  }

  if (file.size > 20 * 1024 * 1024) {
    showImportError('Dung lượng file vượt quá giới hạn 20MB!');
    return;
  }

  clearImportErrors();
  selectedImportFile = file;

  const nameEl = document.getElementById('import-file-selected-name');
  if (nameEl) {
    nameEl.textContent = `📄 Đã chọn: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
    nameEl.style.display = 'block';
  }

  const btnSubmit = document.getElementById('btn-submit-import');
  if (btnSubmit) {
    btnSubmit.disabled = false;
  }
}

function showImportError(msg) {
  const errEl = document.getElementById('import-file-error');
  if (errEl) {
    errEl.textContent = msg;
    errEl.classList.add('show');
  }
}

function clearImportErrors() {
  const errEl = document.getElementById('import-file-error');
  if (errEl) {
    errEl.textContent = '';
    errEl.classList.remove('show');
  }
}