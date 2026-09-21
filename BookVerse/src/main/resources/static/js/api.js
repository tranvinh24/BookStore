/**
 * BookVerse — API Service & Clean Professional UI Helpers
 */

const API_BASE = '';

// Auth Storage Keys
const TOKEN_KEY = 'bookverse_token';
const ROLE_KEY = 'bookverse_role';
const USERNAME_KEY = 'bookverse_username';
const FULLNAME_KEY = 'bookverse_fullname';

/* ==========================================================================
   Auth State Helpers
   ========================================================================== */
function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getRole() {
  return localStorage.getItem(ROLE_KEY);
}

function getUsername() {
  return localStorage.getItem(USERNAME_KEY);
}

function getFullName() {
  return localStorage.getItem(FULLNAME_KEY);
}

function getUserDisplayName() {
  return getFullName() || getUsername() || 'Tài khoản';
}

function setAuth(token, role, userName, fullName) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  if (role) localStorage.setItem(ROLE_KEY, role);
  if (userName) localStorage.setItem(USERNAME_KEY, userName);
  if (fullName) localStorage.setItem(FULLNAME_KEY, fullName);
}

function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(FULLNAME_KEY);
}

function isLoggedIn() {
  return !!getToken();
}

function isAdmin() {
  return getRole() === 'ROLE_ADMIN';
}

function logout() {
  clearAuth();
  showToast('Đã đăng xuất thành công', 'info');
  setTimeout(() => {
    window.location.href = '/login.html';
  }, 400);
}

/* ==========================================================================
   Core API Fetch Wrapper
   ========================================================================== */
async function apiFetch(path, options = {}) {
  const url = path.startsWith('http') ? path : `${API_BASE}${path}`;
  const headers = options.headers ? { ...options.headers } : {};

  const token = getToken();
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let body = options.body;
  if (body && !(body instanceof FormData) && typeof body === 'object') {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  try {
    const res = await fetch(url, {
      ...options,
      headers,
      body,
    });

    if (res.status === 204) {
      return null;
    }

    const text = await res.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = text;
      }
    }

    if (!res.ok) {
      const err = new Error(data?.message || 'Có lỗi xảy ra trong quá trình xử lý');
      err.status = res.status;
      err.code = data?.code || `HTTP_${res.status}`;
      err.validationErrors = data?.validationErrors || null;
      err.data = data;

      if (err.code === 'UNAUTHENTICATED' || res.status === 401) {
        clearAuth();
        const currentPath = window.location.pathname;
        if (!currentPath.includes('login.html') && !currentPath.includes('register.html')) {
          showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'error');
          setTimeout(() => {
            window.location.href = `/login.html?redirect=${encodeURIComponent(currentPath)}`;
          }, 1000);
        }
      }

      throw err;
    }

    return data;
  } catch (error) {
    if (!error.status) {
      error.message = 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại mạng.';
      error.code = 'NETWORK_ERROR';
    }
    throw error;
  }
}

/* ==========================================================================
   Formatters
   ========================================================================== */
function formatPrice(amount) {
  if (amount === undefined || amount === null) return '0 đ';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
}

function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

/* ==========================================================================
   Enum Label & Ribbon Helpers
   ========================================================================== */
const ORDER_STATUS_CONFIG = {
  PENDING: { label: 'Chờ thanh toán', ribbon: 'ribbon-orange' },
  PAID: { label: 'Đã thanh toán', ribbon: 'ribbon-green' },
  PROCESSING: { label: 'Đang chuẩn bị hàng', ribbon: 'ribbon-blue' },
  SHIPPING: { label: 'Đang giao hàng', ribbon: 'ribbon-blue' },
  DELIVERED: { label: 'Giao thành công', ribbon: 'ribbon-green' },
  CANCELLED: { label: 'Đã hủy', ribbon: 'ribbon-red' }
};

function getOrderStatusBadge(status) {
  const conf = ORDER_STATUS_CONFIG[status] || { label: status, ribbon: 'ribbon-blue' };
  return `<span class="ribbon ${conf.ribbon}">${conf.label}</span>`;
}

function getPaymentMethodLabel(method) {
  const map = {
    COD: 'Thanh toán khi nhận hàng (COD)',
    CREDIT_CARD: 'Thẻ tín dụng / Ghi nợ',
    MOMO: 'Ví MoMo',
    VNPAY: 'Cổng VNPAY / QR Ngân hàng'
  };
  return map[method] || method;
}

function getPaymentStatusBadge(status) {
  if (status === 'SUCCESS') return '<span class="ribbon ribbon-green">Đã thanh toán</span>';
  if (status === 'FAILED') return '<span class="ribbon ribbon-red">Thất bại</span>';
  return '<span class="ribbon ribbon-orange">Chờ thanh toán</span>';
}

/* ==========================================================================
   Clean Toast Notification System (No Emojis)
   ========================================================================== */
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span style="font-size: 0.88rem; flex: 1;">${message}</span>`;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 250);
  }, 3500);
}

/* ==========================================================================
   Clean SVG Icons
   ========================================================================== */
const ICONS = {
  search: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>`,
  heart: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`,
  cart: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>`,
  user: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>`,
  menu: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>`,
  bookLogo: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>`
};

/* ==========================================================================
   Clean Professional Navbar Rendering
   ========================================================================== */
function renderNavbar() {
  const navContainer = document.querySelector('.header');
  if (!navContainer) return;

  const authenticated = isLoggedIn();
  const adminUser = isAdmin();
  const currentPath = window.location.pathname;

  navContainer.innerHTML = `
    <!-- Top Bar -->
    <div class="top-bar">
      <div class="container top-bar-inner">
        <span>Nhà sách trực tuyến Góc Của Vin — Sách mở ra thế giới tự do</span>
        <div style="display: flex; gap: 1.25rem;">
          <a href="/my-orders.html">Tra cứu đơn hàng</a>
          <span>|</span>
          <a href="tel:19006868">Hotline: 1900 6868</a>
        </div>
      </div>
    </div>

    <!-- Main Header -->
    <div class="container header-main">
      <a href="/index.html" class="brand" style="display: flex; align-items: center; gap: 0.65rem; text-decoration: none;">
        <img src="/images/logo.png" alt="Góc Của Vin" style="height: 48px; width: 48px; object-fit: contain; border-radius: 4px; flex-shrink: 0;" />
        <div>
          <div style="line-height: 1.1; font-weight: 800; font-size: 1.35rem; letter-spacing: -0.5px;">
            <span style="color: #1A365D;">Góc của </span><span style="color: #D97706;">Vin</span>
          </div>
          <div style="font-size: 0.65rem; color: var(--text-muted); font-weight: 600; letter-spacing: 0.3px;">SÁCH MỞ RA THẾ GIỚI TỰ DO</div>
        </div>
      </a>

      <!-- Search Box -->
      <div class="header-search">
        <form action="/index.html" method="GET" class="header-search-form" id="global-search-form">
          <input type="text" name="q" class="header-search-input" id="global-search-input" placeholder="Tìm kiếm theo tên sách, tác giả, ISBN..." />
          <button type="submit" class="header-search-btn">
            ${ICONS.search}
            <span>Tìm kiếm</span>
          </button>
        </form>
      </div>

      <!-- Action Utilities -->
      <div class="header-actions">

        <!-- Wishlist -->
        <a href="/wishlist.html" class="header-action-item" title="Sách yêu thích">
          <div class="action-icon-circle">
            ${ICONS.heart}
            <span id="nav-wishlist-count" class="action-badge" style="display:none;">0</span>
          </div>
          <div class="action-text">
            <span class="action-text-top">Sách</span>
            <span class="action-text-bottom">Yêu thích</span>
          </div>
        </a>

        <!-- Cart -->
        <a href="/cart.html" class="header-action-item" title="Giỏ hàng">
          <div class="action-icon-circle">
            ${ICONS.cart}
            <span id="nav-cart-count" class="action-badge" style="display:none;">0</span>
          </div>
          <div class="action-text">
            <span class="action-text-top">Giỏ hàng</span>
            <span class="action-text-bottom" id="nav-cart-total">0 sản phẩm</span>
          </div>
        </a>

        <!-- User Dropdown -->
        ${authenticated ? `
          <div class="user-dropdown">
            <div class="header-action-item" id="user-menu-btn">
              <div class="action-icon-circle" style="background: var(--primary); color: #fff; font-weight: 700; font-size: 0.95rem;">
                ${(getUserDisplayName()).charAt(0).toUpperCase()}
              </div>
              <div class="action-text">
                <span class="action-text-top">${adminUser ? 'Quản trị viên' : 'Xin chào,'}</span>
                <span class="action-text-bottom" id="nav-user-display-name">${escapeHtml(getUserDisplayName())} ▾</span>
              </div>
            </div>
            <div class="dropdown-menu" id="user-menu-dropdown">
              <a href="/profile.html" class="dropdown-item">Thông tin tài khoản</a>
              <a href="/my-orders.html" class="dropdown-item">Lịch sử đơn mua</a>
              <a href="/wishlist.html" class="dropdown-item">Sách yêu thích</a>
              ${adminUser ? `<a href="/admin/dashboard.html" class="dropdown-item" style="color: var(--primary); font-weight: 700;">Quản trị hệ thống</a>` : ''}
              <div class="dropdown-divider"></div>
              <a href="javascript:void(0)" onclick="logout()" class="dropdown-item" style="color: var(--primary);">Đăng xuất</a>
            </div>
          </div>
        ` : `
          <a href="/login.html" class="header-action-item">
            <div class="action-icon-circle">${ICONS.user}</div>
            <div class="action-text">
              <span class="action-text-top">Tài khoản</span>
              <span class="action-text-bottom">Đăng nhập</span>
            </div>
          </a>
        `}

      </div>
    </div>

    <!-- Navigation Bar with Category Dropdown -->
    <div class="nav-bar">
      <div class="container nav-bar-inner">
        
        <div class="nav-category-wrap">
          <a href="javascript:void(0)" class="nav-category-btn" id="nav-category-btn">
            ${ICONS.menu}
            <span>TẤT CẢ DANH MỤC SÁCH</span>
            <span style="font-size: 0.75rem; margin-left: 2px;">▾</span>
          </a>
          <ul class="nav-category-dropdown" id="nav-category-dropdown">
            <li class="nav-category-item"><a href="/index.html">Tất cả sách</a></li>
          </ul>
        </div>

        <ul class="nav-links">
          <li><a href="/index.html" class="${currentPath === '/' || currentPath.includes('index.html') ? 'active' : ''}">Trang chủ</a></li>
          <li><a href="/about.html" class="${currentPath.includes('about.html') ? 'active' : ''}">Giới thiệu</a></li>
          ${adminUser ? `
            <li><a href="/admin/dashboard.html" style="color: var(--primary);"><span class="ribbon ribbon-red">Admin Portal</span></a></li>
          ` : ''}
        </ul>
      </div>
    </div>
  `;

  // Dropdown toggle logic
  const menuBtn = document.getElementById('user-menu-btn');
  const dropdown = document.getElementById('user-menu-dropdown');
  if (menuBtn && dropdown) {
    menuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      dropdown.classList.toggle('show');
    });
    document.addEventListener('click', () => {
      dropdown.classList.remove('show');
    });
  }

  loadNavbarCategories();

  if (authenticated) {
    updateBadges();
  }
}

async function loadNavbarCategories() {
  const list = document.getElementById('nav-category-dropdown');
  if (!list) return;

  try {
    const cats = await apiFetch('/api/books/categories');
    if (Array.isArray(cats)) {
      cats.forEach(cat => {
        const li = document.createElement('li');
        li.className = 'nav-category-item';
        li.innerHTML = `<a href="/index.html?category=${encodeURIComponent(cat)}">${cat}</a>`;
        list.appendChild(li);
      });
    }
  } catch (e) { }
}

async function updateBadges() {
  try {
    const [cart, wishlist] = await Promise.all([
      apiFetch('/api/cart').catch(() => null),
      apiFetch('/api/wishlist').catch(() => null)
    ]);

    const cartBadge = document.getElementById('nav-cart-count');
    if (cartBadge && cart && cart.totalItems > 0) {
      cartBadge.textContent = cart.totalItems;
      cartBadge.style.display = 'flex';
      const cartTotal = document.getElementById('nav-cart-total');
      if (cartTotal) cartTotal.textContent = `${cart.totalItems} cuốn`;
    }

    const wishBadge = document.getElementById('nav-wishlist-count');
    if (wishBadge && wishlist && wishlist.length > 0) {
      wishBadge.textContent = wishlist.length;
      wishBadge.style.display = 'flex';
    }
  } catch (e) { }
}

document.addEventListener('DOMContentLoaded', () => {
  renderNavbar();

  // Đồng bộ avatar trên navbar cho tất cả trang (nếu đã đăng nhập)
  if (isLoggedIn()) {
    updateNavbarAvatarFromProfile();
  }
});

/**
 * Lấy thông tin avatar & họ tên từ /api/profile và cập nhật navbar.
 * Chạy trên mọi trang sau khi renderNavbar() xong.
 */
async function updateNavbarAvatarFromProfile() {
  try {
    const profile = await apiFetch('/api/profile');
    if (!profile) return;

    // Cập nhật họ tên hiển thị của user (fullName thay vì username)
    if (profile.fullName) {
      localStorage.setItem(FULLNAME_KEY, profile.fullName);
    }
    const displayName = profile.fullName || profile.userName || getUsername() || 'Tài khoản';
    const nameEl = document.getElementById('nav-user-display-name');
    if (nameEl) {
      nameEl.textContent = displayName + ' ▾';
    }

    const initial = displayName.charAt(0).toUpperCase();
    const circle = document.querySelector('#user-menu-btn .action-icon-circle');
    if (!circle) return;

    // Cập nhật avatar nếu có avatarPath hợp lệ
    if (profile.avatarPath && profile.avatarPath.startsWith('/')) {
      const url = profile.avatarPath + '?t=' + Date.now();
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
  } catch (e) {
    // Không cập nhật được — giữ nguyên
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}