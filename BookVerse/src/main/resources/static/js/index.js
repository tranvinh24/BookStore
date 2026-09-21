// State for Catalog Page
let state = {
  page: 0,
  size: 12,
  sort: 'title',
  dir: 'asc',
  q: '',
  category: ''
};

let userWishlistBookIds = new Set();
let currentSlideIndex = 0;
let slideInterval = null;

const HEART_OUTLINE_SVG = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
const HEART_FILLED_SVG = `<svg width="16" height="16" viewBox="0 0 24 24" fill="var(--primary)" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;

document.addEventListener('DOMContentLoaded', async () => {
  initBannerSlider();
  await loadUserWishlist();

  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('q')) state.q = urlParams.get('q');
  if (urlParams.get('category')) state.category = urlParams.get('category');
  if (urlParams.get('sort')) {
    const parts = urlParams.get('sort').split(':');
    state.sort = parts[0] || 'title';
    state.dir = parts[1] || 'asc';
    document.getElementById('sort-select').value = urlParams.get('sort');
  }
  if (urlParams.get('page')) state.page = parseInt(urlParams.get('page')) || 0;

  if (state.q) {
    const globalInput = document.getElementById('global-search-input');
    if (globalInput) globalInput.value = state.q;
  }

  updateMainTitle();
  bindEvents();
  await loadBooks();
  await loadShowcases();
});

function initBannerSlider() {
  const slides = document.getElementById('carousel-slides');
  const dots = document.querySelectorAll('.carousel-dot');
  const prevBtn = document.getElementById('carousel-prev-btn');
  const nextBtn = document.getElementById('carousel-next-btn');

  if (!slides) return;

  const totalSlides = 3;

  function goToSlide(index) {
    currentSlideIndex = (index + totalSlides) % totalSlides;
    slides.style.transform = `translateX(-${currentSlideIndex * 33.333333}%)`;
    dots.forEach((d, i) => d.classList.toggle('active', i === currentSlideIndex));
  }

  function startAutoPlay() {
    stopAutoPlay();
    slideInterval = setInterval(() => {
      goToSlide(currentSlideIndex + 1);
    }, 4500);
  }

  function stopAutoPlay() {
    if (slideInterval) clearInterval(slideInterval);
  }

  if (prevBtn) {
    prevBtn.addEventListener('click', () => {
      goToSlide(currentSlideIndex - 1);
      startAutoPlay();
    });
  }

  if (nextBtn) {
    nextBtn.addEventListener('click', () => {
      goToSlide(currentSlideIndex + 1);
      startAutoPlay();
    });
  }

  dots.forEach(dot => {
    dot.addEventListener('click', (e) => {
      const idx = parseInt(e.target.dataset.index) || 0;
      goToSlide(idx);
      startAutoPlay();
    });
  });

  const carousel = document.getElementById('main-banner-carousel');
  if (carousel) {
    carousel.addEventListener('mouseenter', stopAutoPlay);
    carousel.addEventListener('mouseleave', startAutoPlay);
  }

  startAutoPlay();
}

function updateMainTitle() {
  const titleEl = document.getElementById('catalog-main-title');
  if (!titleEl) return;

  if (state.category) {
    titleEl.textContent = `Tủ Sách: ${state.category}`;
  } else if (state.q) {
    titleEl.textContent = `Kết quả tìm kiếm cho "${state.q}"`;
  } else {
    titleEl.textContent = 'Tất Cả Tủ Sách';
  }
}

async function loadUserWishlist() {
  if (!isLoggedIn()) return;
  try {
    const items = await apiFetch('/api/wishlist');
    if (Array.isArray(items)) {
      userWishlistBookIds = new Set(items.map(i => i.bookId || i.id));
      const badge = document.getElementById('nav-wishlist-count');
      if (badge && items.length > 0) {
        badge.textContent = items.length;
        badge.style.display = 'flex';
      }
    }
  } catch (e) {}
}

async function loadBooks() {
  const grid = document.getElementById('books-grid');
  const empty = document.getElementById('books-empty');
  const countEl = document.getElementById('results-count');
  const pagWrap = document.getElementById('pagination-wrap');

  grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem;"><p>Đang tải danh sách sách...</p></div>';
  empty.style.display = 'none';

  const params = new URLSearchParams();
  params.set('page', state.page);
  params.set('size', state.size);
  params.set('sort', state.sort);
  params.set('dir', state.dir);

  let endpoint = '/api/books';
  if (state.q) {
    endpoint = '/api/books/search';
    params.set('q', state.q);
  }
  if (state.category) params.set('category', state.category);

  try {
    const data = await apiFetch(`${endpoint}?${params.toString()}`);
    renderActiveFilterBadges();
    updateMainTitle();

    if (!data || !data.content || data.content.length === 0) {
      grid.innerHTML = '';
      empty.style.display = 'block';
      countEl.textContent = '0 kết quả';
      pagWrap.style.display = 'none';
      return;
    }

    countEl.textContent = `Hiển thị ${data.content.length} / ${data.totalElements} cuốn`;
    renderBooksGrid(data.content);
    renderPagination(data);

    // Show/hide showcase sections during active keyword search or category filter
    const bestSection = document.getElementById('section-bestsellers');
    const newSection = document.getElementById('section-new-releases');
    if (state.q || state.category) {
      if (bestSection) bestSection.style.display = 'none';
      if (newSection) newSection.style.display = 'none';
    } else {
      if (bestSection) bestSection.style.display = 'block';
      if (newSection) newSection.style.display = 'block';
    }

  } catch (err) {
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2rem;"><p style="color: var(--primary);">Không thể tải dữ liệu: ${err.message}</p></div>`;
  }
}

function createBookCard(book, ts) {
  const isWishlisted = userWishlistBookIds.has(book.id);
  const stockRibbon = book.stock > 5 
    ? '<span class="ribbon ribbon-green book-card-corner-ribbon">Còn hàng</span>'
    : (book.stock > 0 
        ? `<span class="ribbon ribbon-orange book-card-corner-ribbon">Chỉ còn ${book.stock}</span>`
        : '<span class="ribbon ribbon-red book-card-corner-ribbon">Hết hàng</span>');

  const card = document.createElement('div');
  card.className = 'book-card';
  card.innerHTML = `
    ${stockRibbon}
    <a href="/book-detail.html?id=${book.id}" class="book-card-cover">
      <img src="/api/books/${book.id}/cover?size=medium&v=${ts}" 
           alt="${escapeHtml(book.title)}" 
           onerror="this.parentElement.innerHTML='<div style=\\'height:100%;display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:0.85rem;\\'>Chưa có ảnh bìa</div>'" />
    </a>
    <div class="book-card-content">
      <div class="book-card-category">${escapeHtml(book.category || 'Sách hay')}</div>
      <a href="/book-detail.html?id=${book.id}">
        <h3 class="book-card-title" title="${escapeHtml(book.title)}">${escapeHtml(book.title)}</h3>
      </a>
      <div class="book-card-author">${escapeHtml(book.author || 'Tác giả chưa rõ')}</div>
      
      <div class="book-card-rating">
        <span>★</span>
        <span>${book.rating ? book.rating.toFixed(1) : '5.0'}</span>
        ${book.year ? `<span style="color: var(--text-muted); font-weight: normal; margin-left: auto;">(${book.year})</span>` : ''}
      </div>

      <div class="book-card-bottom">
        <div class="book-price">${formatPrice(book.price)}</div>
        <div class="book-card-actions">
          <button class="btn btn-outline btn-icon btn-sm btn-wishlist ${isWishlisted ? 'active' : ''}" 
                  data-id="${book.id}" 
                  title="${isWishlisted ? 'Bỏ yêu thích' : 'Lưu yêu thích'}">
            ${isWishlisted ? HEART_FILLED_SVG : HEART_OUTLINE_SVG}
          </button>
          <button class="btn btn-primary btn-sm btn-add-cart" 
                  data-id="${book.id}" 
                  ${book.stock <= 0 ? 'disabled' : ''}>
            ${book.stock > 0 ? 'Mua ngay' : 'Hết hàng'}
          </button>
        </div>
      </div>
    </div>
  `;

  card.querySelector('.btn-add-cart')?.addEventListener('click', (e) => handleAddToCart(e.target.dataset.id));
  card.querySelector('.btn-wishlist')?.addEventListener('click', (e) => handleToggleWishlist(e.currentTarget.dataset.id, e.currentTarget));

  return card;
}

function renderBooksGrid(books) {
  const grid = document.getElementById('books-grid');
  grid.innerHTML = '';
  const ts = Date.now();

  books.forEach(book => {
    grid.appendChild(createBookCard(book, ts));
  });
}

async function loadShowcases() {
  const ts = Date.now();

  // Load Bestsellers (4 books, 1 full row)
  const bestGrid = document.getElementById('bestsellers-grid');
  const bestSection = document.getElementById('section-bestsellers');
  if (bestGrid) {
    try {
      const data = await apiFetch('/api/books?page=0&size=4&sort=rating&dir=desc');
      if (data && data.content && data.content.length > 0) {
        bestGrid.innerHTML = '';
        data.content.forEach(book => bestGrid.appendChild(createBookCard(book, ts)));
        if (bestSection) bestSection.style.display = 'block';
      } else if (bestSection) {
        bestSection.style.display = 'none';
      }
    } catch (e) {
      if (bestSection) bestSection.style.display = 'none';
    }
  }

  // Load New Releases (4 books, 1 full row)
  const newGrid = document.getElementById('new-releases-grid');
  const newSection = document.getElementById('section-new-releases');
  if (newGrid) {
    try {
      const data = await apiFetch('/api/books?page=0&size=4&sort=year&dir=desc');
      if (data && data.content && data.content.length > 0) {
        newGrid.innerHTML = '';
        data.content.forEach(book => newGrid.appendChild(createBookCard(book, ts)));
        if (newSection) newSection.style.display = 'block';
      } else if (newSection) {
        newSection.style.display = 'none';
      }
    } catch (e) {
      if (newSection) newSection.style.display = 'none';
    }
  }
}

async function handleAddToCart(bookId) {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để thêm sách vào giỏ hàng!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    }, 800);
    return;
  }

  try {
    await apiFetch('/api/cart', {
      method: 'POST',
      body: { bookId, quantity: 1 }
    });
    showToast('Đã thêm sách vào giỏ hàng!', 'success');
    updateBadges();
  } catch (err) {
    showToast(err.message || 'Không thể thêm vào giỏ', 'error');
  }
}

async function handleToggleWishlist(bookId, btnEl) {
  if (!isLoggedIn()) {
    showToast('Vui lòng đăng nhập để lưu sách yêu thích!', 'warning');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    }, 800);
    return;
  }

  const isFavorited = userWishlistBookIds.has(bookId);
  try {
    if (isFavorited) {
      await apiFetch(`/api/wishlist/${bookId}`, { method: 'DELETE' });
      userWishlistBookIds.delete(bookId);
      btnEl.innerHTML = HEART_OUTLINE_SVG;
      showToast('Đã xóa khỏi danh sách yêu thích', 'info');
    } else {
      await apiFetch('/api/wishlist', {
        method: 'POST',
        body: { bookId }
      });
      userWishlistBookIds.add(bookId);
      btnEl.innerHTML = HEART_FILLED_SVG;
      showToast('Đã thêm vào danh sách yêu thích!', 'success');
    }

    const badge = document.getElementById('nav-wishlist-count');
    if (badge) {
      badge.textContent = userWishlistBookIds.size;
      badge.style.display = userWishlistBookIds.size > 0 ? 'flex' : 'none';
    }
  } catch (err) {
    showToast(err.message || 'Thao tác không thành công', 'error');
  }
}

function renderPagination(data) {
  const wrap = document.getElementById('pagination-wrap');
  if (data.totalPages <= 1) {
    wrap.style.display = 'none';
    return;
  }

  wrap.style.display = 'flex';
  wrap.innerHTML = '';

  const prevBtn = document.createElement('button');
  prevBtn.className = 'page-btn';
  prevBtn.innerHTML = '‹';
  prevBtn.disabled = data.page === 0;
  prevBtn.onclick = () => { state.page--; loadBooks(); };
  wrap.appendChild(prevBtn);

  for (let i = 0; i < data.totalPages; i++) {
    if (data.totalPages > 7 && Math.abs(data.page - i) > 2 && i !== 0 && i !== data.totalPages - 1) {
      if (i === 1 || i === data.totalPages - 2) {
        const dot = document.createElement('span');
        dot.textContent = '...';
        dot.style.padding = '0 0.3rem';
        wrap.appendChild(dot);
      }
      continue;
    }

    const btn = document.createElement('button');
    btn.className = `page-btn ${data.page === i ? 'active' : ''}`;
    btn.textContent = i + 1;
    btn.onclick = () => { state.page = i; loadBooks(); };
    wrap.appendChild(btn);
  }

  const nextBtn = document.createElement('button');
  nextBtn.className = 'page-btn';
  nextBtn.innerHTML = '›';
  nextBtn.disabled = data.last || data.page >= data.totalPages - 1;
  nextBtn.onclick = () => { state.page++; loadBooks(); };
  wrap.appendChild(nextBtn);
}

function renderActiveFilterBadges() {
  const bar = document.getElementById('active-filters-bar');
  bar.innerHTML = '';

  if (state.q) {
    bar.appendChild(createFilterBadge(`Từ khóa: "${state.q}"`, () => {
      state.q = '';
      const input = document.getElementById('global-search-input');
      if (input) input.value = '';
      state.page = 0;
      loadBooks();
    }));
  }
  if (state.category) {
    bar.appendChild(createFilterBadge(`Thể loại: ${state.category}`, () => {
      state.category = '';
      state.page = 0;
      loadBooks();
    }));
  }
}

function createFilterBadge(label, onRemove) {
  const badge = document.createElement('span');
  badge.className = 'ribbon ribbon-blue';
  badge.style.cursor = 'pointer';
  badge.innerHTML = `${label} <span style="margin-left: 6px; font-weight: bold;">✕</span>`;
  badge.onclick = onRemove;
  return badge;
}

function bindEvents() {
  document.getElementById('sort-select').addEventListener('change', (e) => {
    const [field, dir] = e.target.value.split(':');
    state.sort = field;
    state.dir = dir;
    state.page = 0;
    loadBooks();
  });

  const resetAll = () => {
    state.q = '';
    state.category = '';
    state.page = 0;
    const input = document.getElementById('global-search-input');
    if (input) input.value = '';
    loadBooks();
  };

  document.getElementById('btn-empty-reset').addEventListener('click', resetAll);
}