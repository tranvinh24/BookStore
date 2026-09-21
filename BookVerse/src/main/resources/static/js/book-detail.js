let currentBook = null;
let isWishlisted = false;

const HEART_OUTLINE_SVG = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
const HEART_FILLED_SVG = `<svg width="20" height="20" viewBox="0 0 24 24" fill="var(--primary)" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;

document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search);
  const bookId = params.get('id');

  if (!bookId) {
    showError();
    return;
  }

  await Promise.all([
    loadBookDetail(bookId),
    checkWishlistStatus(bookId),
    loadBookReviews(bookId)
  ]);
  bindEvents();
});

async function loadBookDetail(bookId) {
  try {
    currentBook = await apiFetch(`/api/books/${bookId}`);
    renderBook(currentBook);
  } catch (err) {
    showError();
  }
}

async function checkWishlistStatus(bookId) {
  if (!isLoggedIn()) return;
  try {
    const items = await apiFetch('/api/wishlist');
    if (Array.isArray(items)) {
      isWishlisted = items.some(i => (i.bookId || i.id) === bookId);
      updateWishlistButtonUI();
    }
  } catch (e) {}
}

function renderBook(book) {
  document.title = `${book.title} — Góc Của Vin`;

  document.getElementById('crumb-category').textContent = book.category || 'Sách';
  document.getElementById('crumb-title').textContent = book.title;

  document.getElementById('book-title').textContent = book.title;
  document.getElementById('book-author').textContent = book.author || 'Tác giả chưa rõ';
  document.getElementById('book-price').textContent = formatPrice(book.price);
  document.getElementById('book-rating').textContent = `${book.rating ? book.rating.toFixed(1) : '5.0'}`;
  document.getElementById('book-stock-count').textContent = `${book.stock || 0} cuốn`;

  document.getElementById('book-isbn').textContent = book.isbn || 'Chưa cập nhật';
  document.getElementById('book-year').textContent = book.year || 'Chưa cập nhật';
  document.getElementById('book-category-text').textContent = book.category || 'Chưa cập nhật';
  document.getElementById('book-category-ribbon').textContent = book.category || 'Sách hay';

  if (book.description) {
    document.getElementById('book-description').textContent = book.description;
  }

  const coverImg = document.getElementById('book-cover-img');
  coverImg.src = `/api/books/${book.id}/cover?size=large&v=${Date.now()}`;
  coverImg.onerror = () => {
    coverImg.parentElement.innerHTML = `<div style="height: 380px; display: flex; align-items: center; justify-content: center; color: var(--text-muted); background: #F9FAFB;">Chưa có ảnh bìa</div>`;
  };

  const stockRibbon = document.getElementById('book-stock-ribbon');
  const btnAdd = document.getElementById('btn-add-to-cart');
  const btnBuy = document.getElementById('btn-buy-now');
  const qtyInput = document.getElementById('qty-input');

  if (book.stock <= 0) {
    stockRibbon.className = 'ribbon ribbon-red';
    stockRibbon.textContent = 'Hết hàng';
    btnAdd.disabled = true;
    btnAdd.textContent = 'Hết hàng';
    btnBuy.disabled = true;
    qtyInput.disabled = true;
  } else if (book.stock <= 5) {
    stockRibbon.className = 'ribbon ribbon-orange';
    stockRibbon.textContent = `Chỉ còn ${book.stock} cuốn`;
    qtyInput.max = book.stock;
  } else {
    stockRibbon.className = 'ribbon ribbon-green';
    stockRibbon.textContent = 'Còn hàng';
    qtyInput.max = book.stock;
  }

  document.getElementById('detail-loading').style.display = 'none';
  document.getElementById('detail-content').style.display = 'grid';
}

/* ==========================================================================
   Reviews & Ratings Logic
   ========================================================================== */
async function loadBookReviews(bookId) {
  const loadingEl = document.getElementById('reviews-loading');
  const emptyEl = document.getElementById('reviews-empty');
  const listEl = document.getElementById('reviews-list');

  if (loadingEl) loadingEl.style.display = 'block';
  if (emptyEl) emptyEl.style.display = 'none';
  if (listEl) listEl.innerHTML = '';

  // Check login state for review form vs prompt
  const formContainer = document.getElementById('review-form-container');
  const loginPrompt = document.getElementById('review-login-prompt');
  if (isLoggedIn()) {
    if (formContainer) formContainer.style.display = 'block';
    if (loginPrompt) loginPrompt.style.display = 'none';
  } else {
    if (formContainer) formContainer.style.display = 'none';
    if (loginPrompt) loginPrompt.style.display = 'flex';
  }

  try {
    const reviews = await apiFetch(`/api/books/${bookId}/reviews`);
    if (loadingEl) loadingEl.style.display = 'none';

    if (!Array.isArray(reviews) || reviews.length === 0) {
      if (emptyEl) emptyEl.style.display = 'block';
      updateReviewSummary(0, 0);
      return;
    }

    // Calculate Average
    const totalRating = reviews.reduce((sum, r) => sum + (r.rating || 5), 0);
    const avgRating = totalRating / reviews.length;
    updateReviewSummary(avgRating, reviews.length);

    renderReviewsList(reviews);

  } catch (err) {
    if (loadingEl) loadingEl.style.display = 'none';
    if (listEl) listEl.innerHTML = `<div style="color: var(--ribbon-coral); padding: 1rem; text-align: center;">Không thể tải bình luận: ${err.message}</div>`;
  }
}

function updateReviewSummary(avg, count) {
  const avgText = count > 0 ? avg.toFixed(1) : (currentBook && currentBook.rating ? currentBook.rating.toFixed(1) : '5.0');
  const avgEl = document.getElementById('review-summary-avg');
  const countEl = document.getElementById('review-summary-count');
  const starsEl = document.getElementById('review-summary-stars');
  const bookRatingEl = document.getElementById('book-rating');

  if (avgEl) avgEl.textContent = avgText;
  if (countEl) countEl.textContent = `(${count} đánh giá)`;
  if (bookRatingEl) bookRatingEl.textContent = avgText;

  if (starsEl) {
    const roundedStars = Math.round(parseFloat(avgText));
    starsEl.textContent = '★'.repeat(roundedStars) + '☆'.repeat(5 - roundedStars);
  }
}

function renderReviewsList(reviews) {
  const listEl = document.getElementById('reviews-list');
  if (!listEl) return;
  listEl.innerHTML = '';

  const currentUsername = getUsername();
  const userIsAdmin = isAdmin();

  reviews.forEach(r => {
    const item = document.createElement('div');
    item.className = 'review-item';

    const formattedDate = r.createdAt
      ? new Date(r.createdAt).toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
      : 'Vừa xong';

    const goldStars = '★'.repeat(r.rating || 5);
    const emptyStars = '☆'.repeat(5 - (r.rating || 5));

    const isOwn = currentUsername && currentUsername === r.userName;
    const canDelete = isOwn || userIsAdmin;
    const displayName = r.userFullName || r.userName || 'Độc giả';
    const fallbackAvatar = 'https://ui-avatars.com/api/?name=' + encodeURIComponent(displayName) + '&background=random&color=fff&size=128&bold=true';
    let avatarSrc = r.avatarUrl;
    if (!avatarSrc || (!avatarSrc.startsWith('/') && !avatarSrc.startsWith('http'))) {
      avatarSrc = fallbackAvatar;
    }

    item.innerHTML = `
      <div style="display: flex; gap: 1rem; align-items: flex-start;">
        <img src="${avatarSrc}" 
             alt="${escapeHtml(displayName)}" 
             style="width: 44px; height: 44px; border-radius: 50%; object-fit: cover; border: 1px solid var(--border); flex-shrink: 0;"
             onerror="this.src='${fallbackAvatar}'; this.onerror=null;" />
        
        <div style="flex: 1;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.25rem;">
            <div>
              <strong style="color: var(--text-main); font-size: 0.95rem;">${escapeHtml(displayName)}</strong>
              ${isOwn ? '<span class="ribbon ribbon-blue" style="font-size: 0.72rem; padding: 0.1rem 0.4rem; margin-left: 0.4rem;">Bạn</span>' : ''}
              <span style="font-size: 0.78rem; color: var(--text-muted); margin-left: 0.6rem;">${formattedDate}</span>
            </div>
            ${canDelete ? `<button class="btn btn-outline btn-sm btn-delete-review" data-id="${r.id}" style="color: var(--primary); border-color: var(--border); font-size: 0.75rem; padding: 0.15rem 0.45rem;">Xóa</button>` : ''}
          </div>

          <div style="margin-bottom: 0.4rem;">
            <span class="gold-stars" style="font-size: 0.95rem;">${goldStars}</span><span style="color: #D1D5DB; letter-spacing: 2px; font-size: 0.95rem;">${emptyStars}</span>
          </div>

          <div style="color: var(--text-secondary); font-size: 0.92rem; line-height: 1.6; white-space: pre-wrap;">${escapeHtml(r.comment || '')}</div>
        </div>
      </div>
    `;

    listEl.appendChild(item);
  });

  // Bind delete events
  document.querySelectorAll('.btn-delete-review').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (!confirm('Bạn có chắc chắn muốn xóa nhận xét này?')) return;
      try {
        await apiFetch(`/api/reviews/${btn.dataset.id}`, { method: 'DELETE' });
        showToast('Đã xóa nhận xét thành công', 'info');
        loadBookReviews(currentBook.id);
      } catch (err) {
        showToast(err.message || 'Không thể xóa nhận xét', 'error');
      }
    });
  });
}

function escapeHtml(str) {
  return str.replace(/[&<>'"]/g, tag => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
  }[tag] || tag));
}

function showError() {
  document.getElementById('detail-loading').style.display = 'none';
  document.getElementById('detail-content').style.display = 'none';
  document.getElementById('detail-error').style.display = 'block';
}

function updateWishlistButtonUI() {
  const btn = document.getElementById('btn-wishlist-toggle');
  if (isWishlisted) {
    btn.innerHTML = HEART_FILLED_SVG;
    btn.style.color = 'var(--primary)';
    btn.style.borderColor = 'var(--primary)';
  } else {
    btn.innerHTML = HEART_OUTLINE_SVG;
    btn.style.color = '';
    btn.style.borderColor = '';
  }
}

function bindEvents() {
  const qtyInput = document.getElementById('qty-input');

  document.getElementById('btn-qty-minus').addEventListener('click', () => {
    let val = parseInt(qtyInput.value) || 1;
    if (val > 1) qtyInput.value = val - 1;
  });

  document.getElementById('btn-qty-plus').addEventListener('click', () => {
    let val = parseInt(qtyInput.value) || 1;
    const max = currentBook ? currentBook.stock : 99;
    if (val < max) qtyInput.value = val + 1;
    else showToast(`Kho chỉ còn tối đa ${max} cuốn!`, 'warning');
  });

  document.getElementById('btn-add-to-cart').addEventListener('click', async () => {
    await handleCartAction(false);
  });

  document.getElementById('btn-buy-now').addEventListener('click', async () => {
    await handleCartAction(true);
  });

  document.getElementById('btn-wishlist-toggle').addEventListener('click', async () => {
    if (!isLoggedIn()) {
      showToast('Vui lòng đăng nhập để lưu sách yêu thích!', 'warning');
      setTimeout(() => {
        window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.href)}`;
      }, 800);
      return;
    }

    try {
      if (isWishlisted) {
        await apiFetch(`/api/wishlist/${currentBook.id}`, { method: 'DELETE' });
        isWishlisted = false;
        showToast('Đã xóa khỏi danh sách yêu thích', 'info');
      } else {
        await apiFetch('/api/wishlist', {
          method: 'POST',
          body: { bookId: currentBook.id }
        });
        isWishlisted = true;
        showToast('Đã thêm vào danh sách yêu thích!', 'success');
      }
      updateWishlistButtonUI();
    } catch (err) {
      showToast(err.message || 'Thao tác không thành công', 'error');
    }
  });

  // Review star selection label update
  const starLabels = {
    '5': '5 sao (Tuyệt vời)',
    '4': '4 sao (Rất hay)',
    '3': '3 sao (Bình thường)',
    '2': '2 sao (Chưa ưng ý)',
    '1': '1 sao (Tệ)'
  };
  document.querySelectorAll('input[name="rating"]').forEach(radio => {
    radio.addEventListener('change', (e) => {
      const starText = document.getElementById('selected-star-text');
      if (starText) starText.textContent = starLabels[e.target.value] || `${e.target.value} sao`;
    });
  });

  // Review Login prompt click
  const btnReviewLogin = document.getElementById('btn-review-login');
  if (btnReviewLogin) {
    btnReviewLogin.addEventListener('click', () => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.href)}`;
    });
  }

  // Submit Review Form
  const reviewForm = document.getElementById('review-form');
  if (reviewForm) {
    reviewForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để gửi đánh giá!', 'warning');
        return;
      }

      const selectedRating = document.querySelector('input[name="rating"]:checked');
      const ratingVal = selectedRating ? parseInt(selectedRating.value) : 5;
      const commentVal = document.getElementById('review-comment-input').value.trim();
      const errEl = document.getElementById('review-error-msg');
      const btnSubmit = document.getElementById('btn-submit-review');

      if (!commentVal) {
        if (errEl) {
          errEl.textContent = 'Vui lòng nhập nội dung nhận xét của bạn!';
          errEl.classList.add('show');
        }
        return;
      }

      if (errEl) errEl.classList.remove('show');
      btnSubmit.disabled = true;
      btnSubmit.textContent = 'Đang gửi...';

      try {
        await apiFetch(`/api/books/${currentBook.id}/reviews`, {
          method: 'POST',
          body: {
            rating: ratingVal,
            comment: commentVal
          }
        });

        showToast('Cảm ơn bạn đã gửi đánh giá cuốn sách!', 'success');
        document.getElementById('review-comment-input').value = '';
        await loadBookReviews(currentBook.id);
      } catch (err) {
        if (errEl) {
          errEl.textContent = err.message || 'Không thể gửi đánh giá. Vui lòng thử lại.';
          errEl.classList.add('show');
        } else {
          showToast(err.message || 'Lỗi gửi đánh giá', 'error');
        }
      } finally {
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Gửi bình luận & Đánh giá';
      }
    });
  }
}

async function handleCartAction(redirectCheckout = false) {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để thực hiện mua sách!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.href)}`;
    }, 800);
    return;
  }

  const qty = parseInt(document.getElementById('qty-input').value) || 1;

  try {
    await apiFetch('/api/cart', {
      method: 'POST',
      body: {
        bookId: currentBook.id,
        quantity: qty
      }
    });

    if (redirectCheckout) {
      window.location.href = '/checkout.html';
    } else {
      showToast(`Đã thêm ${qty} cuốn vào giỏ hàng!`, 'success');
      updateBadges();
    }
  } catch (err) {
    showToast(err.message || 'Không thể thêm vào giỏ hàng', 'error');
  }
}