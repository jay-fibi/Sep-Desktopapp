/**
 * FreshCart - Main Application Controller
 */
(function (global) {
  "use strict";

  const store = global.FreshStore;

  // DOM Elements
  const DOM = {};

  function initElements() {
    DOM.themeToggle = document.getElementById("theme-toggle");
    DOM.cartToggleBtn = document.getElementById("cart-toggle-btn");
    DOM.navCartCount = document.getElementById("nav-cart-count");
    DOM.navFavoritesBtn = document.getElementById("nav-favorites-btn");
    DOM.navFavoritesCount = document.getElementById("nav-favorites-count");
    DOM.navOrdersBtn = document.getElementById("nav-orders-btn");
    DOM.navOrdersCount = document.getElementById("nav-orders-count");

    DOM.searchInput = document.getElementById("search-input");
    DOM.searchClearBtn = document.getElementById("search-clear-btn");
    DOM.categoryStrip = document.getElementById("category-strip");

    DOM.priceSlider = document.getElementById("price-slider");
    DOM.priceDisplay = document.getElementById("price-display");
    DOM.dietFilter = document.getElementById("diet-filter");
    DOM.stockFilter = document.getElementById("stock-filter");
    DOM.resetFiltersBtn = document.getElementById("reset-filters-btn");
    DOM.mobileFilterBtn = document.getElementById("mobile-filter-btn");
    DOM.filtersSidebar = document.getElementById("filters-sidebar");

    DOM.sortSelect = document.getElementById("sort-select");
    DOM.resultsCount = document.getElementById("results-count");
    DOM.productsGrid = document.getElementById("products-grid");

    // Cart Drawer
    DOM.cartOverlay = document.getElementById("cart-overlay");
    DOM.cartDrawer = document.getElementById("cart-drawer");
    DOM.cartCloseBtn = document.getElementById("cart-close-btn");
    DOM.cartItemsList = document.getElementById("cart-items-list");
    DOM.cartSubtotal = document.getElementById("cart-subtotal");
    DOM.cartDiscount = document.getElementById("cart-discount");
    DOM.cartDiscountRow = document.getElementById("cart-discount-row");
    DOM.cartTax = document.getElementById("cart-tax");
    DOM.cartDelivery = document.getElementById("cart-delivery");
    DOM.cartTotal = document.getElementById("cart-total");
    DOM.freeDeliveryText = document.getElementById("free-delivery-text");
    DOM.freeDeliveryFill = document.getElementById("free-delivery-fill");
    DOM.couponInput = document.getElementById("coupon-input");
    DOM.applyCouponBtn = document.getElementById("apply-coupon-btn");
    DOM.couponAppliedContainer = document.getElementById("coupon-applied-container");
    DOM.checkoutBtn = document.getElementById("checkout-btn");

    // Checkout Modal
    DOM.checkoutModal = document.getElementById("checkout-modal");
    DOM.checkoutCloseBtn = document.getElementById("checkout-close-btn");
    DOM.checkoutForm = document.getElementById("checkout-form");
    DOM.checkoutMiniSubtotal = document.getElementById("checkout-mini-subtotal");
    DOM.checkoutMiniTotal = document.getElementById("checkout-mini-total");

    // Order Success Modal
    DOM.orderSuccessModal = document.getElementById("order-success-modal");
    DOM.orderSuccessCloseBtn = document.getElementById("order-success-close-btn");
    DOM.orderSuccessId = document.getElementById("order-success-id");
    DOM.orderSuccessDelivery = document.getElementById("order-success-delivery");
    DOM.orderSuccessDoneBtn = document.getElementById("order-success-done-btn");

    // Orders History Modal
    DOM.ordersModal = document.getElementById("orders-modal");
    DOM.ordersCloseBtn = document.getElementById("orders-close-btn");
    DOM.ordersListContainer = document.getElementById("orders-list-container");

    // Product Detail Modal
    DOM.detailModal = document.getElementById("detail-modal");
    DOM.detailCloseBtn = document.getElementById("detail-close-btn");
    DOM.detailContainer = document.getElementById("detail-container");

    // Admin Add Product Modal
    DOM.adminAddBtn = document.getElementById("admin-add-btn");
    DOM.addProductModal = document.getElementById("add-product-modal");
    DOM.addProductCloseBtn = document.getElementById("add-product-close-btn");
    DOM.addProductForm = document.getElementById("add-product-form");

    // Toast Container
    DOM.toastContainer = document.getElementById("toast-container");
  }

  /* ---------------- Toast Notifications ---------------- */
  function showToast(message, type = "success") {
    if (!DOM.toastContainer) return;
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    const icon = type === "error" ? "⚠️" : type === "info" ? "ℹ️" : "✅";
    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    DOM.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transform = "translateY(10px)";
      toast.style.transition = "all 0.2s ease";
      setTimeout(() => toast.remove(), 250);
    }, 3000);
  }

  /* ---------------- Rendering Products ---------------- */
  function renderProducts() {
    const products = store.getFilteredProducts();
    DOM.resultsCount.textContent = products.length;

    if (products.length === 0) {
      DOM.productsGrid.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">🔍</div>
          <h3>No matching groceries found</h3>
          <p>Try clearing filters or searching for something else like bananas, milk, or coffee.</p>
          <button type="button" class="btn-add-cart" id="empty-reset-btn">Reset All Filters</button>
        </div>
      `;
      const emptyReset = document.getElementById("empty-reset-btn");
      if (emptyReset) {
        emptyReset.addEventListener("click", resetAllFilters);
      }
      return;
    }

    DOM.productsGrid.innerHTML = products
      .map((p) => {
        const isFav = store.isFavorite(p.id);
        const isOutOfStock = p.stock <= 0;
        const isLowStock = p.stock > 0 && p.stock <= 5;

        return `
        <article class="product-card" data-id="${p.id}">
          ${p.badge ? `<span class="product-badge">${p.badge}</span>` : ""}
          <button type="button" class="product-favorite-btn ${isFav ? "favorited" : ""}" data-fav-id="${p.id}" aria-label="Add to favorites" title="${isFav ? "Remove favorite" : "Add favorite"}">
            ${isFav ? "❤️" : "🤍"}
          </button>
          
          <div class="product-image-wrap" data-detail-id="${p.id}" title="Click to view details">
            ${p.image}
          </div>
          
          <div class="product-category-tag">${p.category}</div>
          <h3 class="product-title" data-detail-id="${p.id}">${escapeHtml(p.name)}</h3>
          
          <div class="product-rating">
            <span class="stars">★</span> <strong>${p.rating.toFixed(1)}</strong>
            <span>(${p.reviewsCount})</span>
          </div>

          <div class="product-unit">${escapeHtml(p.unit)}</div>

          <div class="product-diet-tags">
            ${(p.dietary || []).map((d) => `<span class="diet-tag">${d}</span>`).join("")}
          </div>

          <div class="product-footer">
            <div>
              <div class="product-price">$${p.price.toFixed(2)}</div>
              <div class="product-stock ${isOutOfStock ? "out-of-stock" : isLowStock ? "low-stock" : ""}">
                ${isOutOfStock ? "Out of Stock" : isLowStock ? `Only ${p.stock} left!` : "In Stock"}
              </div>
            </div>

            <button type="button" class="btn-add-cart" data-add-cart="${p.id}" ${isOutOfStock ? "disabled" : ""}>
              <span>+ Add</span>
            </button>
          </div>
        </article>
      `;
      })
      .join("");
  }

  /* ---------------- Rendering Cart ---------------- */
  function renderCart() {
    const calc = store.getCartCalculations();
    const count = store.getCartTotalCount();

    DOM.navCartCount.textContent = count;

    // Free delivery meter
    if (calc.amountForFreeDelivery > 0 && calc.subtotal > 0) {
      DOM.freeDeliveryText.innerHTML = `Add <strong>$${calc.amountForFreeDelivery.toFixed(2)}</strong> more for <strong>FREE Delivery</strong>!`;
      DOM.freeDeliveryFill.style.width = `${calc.freeDeliveryProgress}%`;
    } else if (calc.subtotal >= 35) {
      DOM.freeDeliveryText.innerHTML = `🎉 You qualify for <strong>FREE Delivery</strong>!`;
      DOM.freeDeliveryFill.style.width = "100%";
    } else {
      DOM.freeDeliveryText.innerHTML = `Orders over $35 get <strong>FREE Delivery</strong>!`;
      DOM.freeDeliveryFill.style.width = "0%";
    }

    if (calc.items.length === 0) {
      DOM.cartItemsList.innerHTML = `
        <div class="cart-empty">
          <div class="cart-empty-icon">🛒</div>
          <h4>Your cart is empty</h4>
          <p>Discover fresh groceries and add items to your cart.</p>
        </div>
      `;
      DOM.cartSubtotal.textContent = "$0.00";
      DOM.cartTax.textContent = "$0.00";
      DOM.cartDelivery.textContent = "$0.00";
      DOM.cartTotal.textContent = "$0.00";
      DOM.cartDiscountRow.style.display = "none";
      DOM.couponAppliedContainer.innerHTML = "";
      DOM.checkoutBtn.disabled = true;
      return;
    }

    DOM.checkoutBtn.disabled = false;
    DOM.cartSubtotal.textContent = `$${calc.subtotal.toFixed(2)}`;
    DOM.cartTax.textContent = `$${calc.tax.toFixed(2)}`;
    DOM.cartDelivery.textContent = calc.deliveryFee === 0 ? "FREE" : `$${calc.deliveryFee.toFixed(2)}`;
    DOM.cartTotal.textContent = `$${calc.total.toFixed(2)}`;

    if (calc.discount > 0) {
      DOM.cartDiscountRow.style.display = "flex";
      DOM.cartDiscount.textContent = `-$${calc.discount.toFixed(2)}`;
    } else {
      DOM.cartDiscountRow.style.display = "none";
    }

    // Coupon pill
    if (calc.coupon) {
      DOM.couponAppliedContainer.innerHTML = `
        <div class="coupon-applied-tag">
          <span>Promo <strong>${calc.coupon.code}</strong> applied (${calc.coupon.desc})</span>
          <button type="button" id="remove-coupon-btn" style="color:var(--danger); font-weight:700; font-size:1.1rem; padding-left:8px;" title="Remove coupon">✕</button>
        </div>
      `;
      document.getElementById("remove-coupon-btn").addEventListener("click", () => {
        store.removeCoupon();
        showToast("Coupon removed", "info");
      });
    } else {
      DOM.couponAppliedContainer.innerHTML = "";
    }

    // Items list
    DOM.cartItemsList.innerHTML = calc.items
      .map(
        (it) => `
      <div class="cart-item">
        <div class="cart-item-img">${it.product.image}</div>
        <div class="cart-item-info">
          <div class="cart-item-title">${escapeHtml(it.product.name)}</div>
          <div class="cart-item-price">$${it.product.price.toFixed(2)} / ${it.product.unit}</div>
          <div class="cart-item-controls">
            <div class="qty-stepper">
              <button type="button" class="qty-btn" data-qty-dec="${it.productId}">-</button>
              <span class="qty-val">${it.quantity}</span>
              <button type="button" class="qty-btn" data-qty-inc="${it.productId}" ${it.quantity >= it.product.stock ? "disabled" : ""}>+</button>
            </div>
            <div class="cart-item-total">$${it.lineTotal.toFixed(2)}</div>
            <button type="button" class="btn-remove-item" data-cart-remove="${it.productId}" title="Remove item">🗑️</button>
          </div>
        </div>
      </div>
    `
      )
      .join("");
  }

  /* ---------------- Nav Badges & Counters ---------------- */
  function updateNavCounters() {
    DOM.navFavoritesCount.textContent = store.favorites.length;
    DOM.navOrdersCount.textContent = store.orders.length;
  }

  /* ---------------- Product Detail Modal ---------------- */
  function openProductDetail(productId) {
    const p = store.getProductById(productId);
    if (!p) return;

    const isFav = store.isFavorite(p.id);
    const isOutOfStock = p.stock <= 0;

    DOM.detailContainer.innerHTML = `
      <div class="detail-modal-body">
        <div class="detail-img-box">${p.image}</div>
        <div>
          <div class="product-category-tag">${p.category}</div>
          <h2 style="font-size:1.5rem; margin-bottom:8px;">${escapeHtml(p.name)}</h2>
          <div class="product-rating" style="margin-bottom:12px;">
            <span class="stars">★</span> <strong>${p.rating.toFixed(1)}</strong>
            <span>(${p.reviewsCount} customer reviews)</span>
          </div>
          <div style="font-size:1.8rem; font-weight:800; color:var(--text-main); margin-bottom:12px;">
            $${p.price.toFixed(2)} <span style="font-size:0.95rem; font-weight:normal; color:var(--text-muted);">/ ${p.unit}</span>
          </div>
          <p style="color:var(--text-muted); line-height:1.6; margin-bottom:16px;">${escapeHtml(p.description)}</p>

          <div class="detail-meta-list">
            <div class="detail-meta-row">
              <span>Origin / Farm</span>
              <strong>${escapeHtml(p.origin || "Local")}</strong>
            </div>
            <div class="detail-meta-row">
              <span>Availability</span>
              <strong style="color:${isOutOfStock ? "var(--danger)" : "var(--primary)"};">${isOutOfStock ? "Out of Stock" : `${p.stock} units available`}</strong>
            </div>
            <div class="detail-meta-row">
              <span>Dietary Traits</span>
              <strong>${(p.dietary || []).join(", ") || "Standard"}</strong>
            </div>
          </div>

          <div style="display:flex; gap:12px; margin-top:20px;">
            <button type="button" class="btn-add-cart" id="modal-add-cart" style="flex:1; justify-content:center; padding:12px;" ${isOutOfStock ? "disabled" : ""}>
              🛒 Add to Cart
            </button>
            <button type="button" class="btn-icon" id="modal-fav-btn" style="width:48px; height:48px;">
              ${isFav ? "❤️" : "🤍"}
            </button>
          </div>
        </div>
      </div>
    `;

    document.getElementById("modal-add-cart").addEventListener("click", () => {
      const res = store.addToCart(p.id, 1);
      if (res && res.success) {
        showToast(`Added ${p.name} to cart!`);
        openCartDrawer();
        DOM.detailModal.classList.remove("active");
      }
    });

    document.getElementById("modal-fav-btn").addEventListener("click", () => {
      const fav = store.toggleFavorite(p.id);
      showToast(fav ? `Saved ${p.name} to favorites` : `Removed ${p.name} from favorites`, "info");
      openProductDetail(p.id);
    });

    DOM.detailModal.classList.add("active");
  }

  /* ---------------- Orders History Modal ---------------- */
  function openOrdersModal() {
    if (store.orders.length === 0) {
      DOM.ordersListContainer.innerHTML = `
        <div class="empty-state" style="padding:40px 10px;">
          <div class="empty-state-icon">📦</div>
          <h4>No orders yet</h4>
          <p>When you place an order, receipt details and tracking will appear here.</p>
        </div>
      `;
    } else {
      DOM.ordersListContainer.innerHTML = store.orders
        .map(
          (ord) => `
        <div class="order-history-card">
          <div class="order-history-header">
            <div>
              <strong>Order #${ord.orderId}</strong>
              <div style="font-size:0.8rem; color:var(--text-muted);">${new Date(ord.placedAt).toLocaleString()}</div>
            </div>
            <span style="background:var(--primary-light); color:var(--primary-dark); font-weight:700; padding:4px 10px; border-radius:var(--radius-full); font-size:0.82rem;">${ord.status}</span>
          </div>

          <div class="order-history-items">
            ${ord.items.map((i) => `<div>${i.quantity}x ${escapeHtml(i.name)} ($${i.lineTotal.toFixed(2)})</div>`).join("")}
          </div>

          <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.9rem; padding-top:6px; border-top:1px dashed var(--border-color);">
            <div>Delivery to: <strong>${escapeHtml(ord.customer.name)}</strong> (${ord.deliverySlot})</div>
            <div style="font-size:1.05rem; font-weight:800; color:var(--primary);">$${ord.pricing.total.toFixed(2)}</div>
          </div>
        </div>
      `
        )
        .join("");
    }

    DOM.ordersModal.classList.add("active");
  }

  /* ---------------- Favorites Filter Toggle ---------------- */
  function toggleFavoritesView() {
    store.favoritesOnly = !store.favoritesOnly;
    if (store.favoritesOnly) {
      DOM.navFavoritesBtn.style.color = "var(--primary)";
      showToast("Showing favorite items only", "info");
    } else {
      DOM.navFavoritesBtn.style.color = "";
      showToast("Showing all products", "info");
    }
    renderProducts();
  }

  /* ---------------- Filter Handling ---------------- */
  function selectCategory(category) {
    store.activeCategory = category;
    document.querySelectorAll("#category-strip .cat-pill").forEach((pill) => {
      pill.classList.toggle("active", pill.dataset.category === category);
    });
    renderProducts();
  }

  function resetAllFilters() {
    store.activeCategory = "all";
    store.searchQuery = "";
    store.selectedDiet = "all";
    store.sortBy = "featured";
    store.inStockOnly = false;
    store.priceRange = 25;
    store.favoritesOnly = false;

    DOM.searchInput.value = "";
    DOM.searchClearBtn.classList.remove("visible");
    DOM.priceSlider.value = 25;
    DOM.priceDisplay.textContent = "$25.00";
    DOM.dietFilter.value = "all";
    DOM.stockFilter.checked = false;
    DOM.sortSelect.value = "featured";
    DOM.navFavoritesBtn.style.color = "";

    document.querySelectorAll(".cat-pill").forEach((pill) => {
      pill.classList.toggle("active", pill.dataset.category === "all");
    });

    renderProducts();
    showToast("Filters reset", "info");
  }

  /* ---------------- Drawer & Modal Helpers ---------------- */
  function openCartDrawer() {
    DOM.cartOverlay.classList.add("active");
    DOM.cartDrawer.classList.add("active");
  }

  function closeCartDrawer() {
    DOM.cartOverlay.classList.remove("active");
    DOM.cartDrawer.classList.remove("active");
  }

  function openCheckoutModal() {
    const calc = store.getCartCalculations();
    if (calc.items.length === 0) return;

    DOM.checkoutMiniSubtotal.textContent = `$${calc.subtotal.toFixed(2)}`;
    DOM.checkoutMiniTotal.textContent = `$${calc.total.toFixed(2)}`;
    closeCartDrawer();
    DOM.checkoutModal.classList.add("active");
  }

  /* ---------------- Event Listeners Setup ---------------- */
  function attachEvents() {
    // Theme toggle
    DOM.themeToggle.addEventListener("click", () => {
      const nextTheme = store.theme === "dark" ? "light" : "dark";
      store.setTheme(nextTheme);
      DOM.themeToggle.textContent = nextTheme === "dark" ? "☀️" : "🌙";
      showToast(`Switched to ${nextTheme} theme`, "info");
    });

    // Cart Drawer Open/Close
    DOM.cartToggleBtn.addEventListener("click", openCartDrawer);
    DOM.cartCloseBtn.addEventListener("click", closeCartDrawer);
    DOM.cartOverlay.addEventListener("click", closeCartDrawer);

    // Coupon code pills click to auto apply
    document.querySelectorAll(".top-coupon-pill").forEach((pill) => {
      pill.addEventListener("click", () => {
        const code = pill.dataset.code;
        if (code) {
          DOM.couponInput.value = code;
          const res = store.applyCoupon(code);
          if (res.success) {
            showToast(`Coupon ${code} applied!`);
            openCartDrawer();
          } else {
            showToast(res.message, "error");
          }
        }
      });
    });

    // Apply coupon button
    DOM.applyCouponBtn.addEventListener("click", () => {
      const code = DOM.couponInput.value;
      if (!code) return;
      const res = store.applyCoupon(code);
      if (res.success) {
        showToast(`Promo code ${code} applied!`);
      } else {
        showToast(res.message, "error");
      }
    });

    // Category Strip Pills
    DOM.categoryStrip.addEventListener("click", (e) => {
      const pill = e.target.closest(".cat-pill");
      if (!pill) return;
      selectCategory(pill.dataset.category);
    });

    // Footer category shortcuts
    document.querySelectorAll("[data-footer-category]").forEach((link) => {
      link.addEventListener("click", (e) => {
        e.preventDefault();
        selectCategory(link.dataset.footerCategory);
        window.scrollTo({ top: 0, behavior: "smooth" });
      });
    });

    // Search bar
    DOM.searchInput.addEventListener("input", (e) => {
      store.searchQuery = e.target.value;
      DOM.searchClearBtn.classList.toggle("visible", Boolean(e.target.value.trim()));
      renderProducts();
    });

    DOM.searchClearBtn.addEventListener("click", () => {
      DOM.searchInput.value = "";
      DOM.searchClearBtn.classList.remove("visible");
      store.searchQuery = "";
      renderProducts();
    });

    // Price Slider
    DOM.priceSlider.addEventListener("input", (e) => {
      const val = parseFloat(e.target.value);
      store.priceRange = val;
      DOM.priceDisplay.textContent = `$${val.toFixed(2)}`;
      renderProducts();
    });

    // Diet Filter
    DOM.dietFilter.addEventListener("change", (e) => {
      store.selectedDiet = e.target.value;
      renderProducts();
    });

    // Stock Filter
    DOM.stockFilter.addEventListener("change", (e) => {
      store.inStockOnly = e.target.checked;
      renderProducts();
    });

    // Sort select
    DOM.sortSelect.addEventListener("change", (e) => {
      store.sortBy = e.target.value;
      renderProducts();
    });

    // Reset Filters Button
    DOM.resetFiltersBtn.addEventListener("click", resetAllFilters);

    // Mobile filter toggle
    DOM.mobileFilterBtn.addEventListener("click", () => {
      DOM.filtersSidebar.classList.toggle("mobile-open");
    });

    // Favorites nav button
    DOM.navFavoritesBtn.addEventListener("click", toggleFavoritesView);

    // Orders nav button
    DOM.navOrdersBtn.addEventListener("click", openOrdersModal);
    DOM.ordersCloseBtn.addEventListener("click", () => DOM.ordersModal.classList.remove("active"));

    // Products Grid Click Delegate (Add to cart, Favorite, Detail)
    DOM.productsGrid.addEventListener("click", (e) => {
      // Favorite button
      const favBtn = e.target.closest("[data-fav-id]");
      if (favBtn) {
        const id = favBtn.dataset.favId;
        const fav = store.toggleFavorite(id);
        const p = store.getProductById(id);
        showToast(fav ? `Saved ${p.name} to favorites` : `Removed ${p.name} from favorites`, "info");
        return;
      }

      // Add to cart button
      const addBtn = e.target.closest("[data-add-cart]");
      if (addBtn) {
        const id = addBtn.dataset.addCart;
        const res = store.addToCart(id, 1);
        if (res.success) {
          const p = store.getProductById(id);
          showToast(`Added ${p.name} to cart!`);
        } else if (res.reason === "exceeds_stock") {
          showToast(`Maximum stock available is ${res.max}`, "error");
        }
        return;
      }

      // Detail click
      const detailTarget = e.target.closest("[data-detail-id]");
      if (detailTarget) {
        openProductDetail(detailTarget.dataset.detailId);
      }
    });

    // Product Detail Modal Close
    DOM.detailCloseBtn.addEventListener("click", () => DOM.detailModal.classList.remove("active"));

    // Cart Items List Stepper / Remove Delegate
    DOM.cartItemsList.addEventListener("click", (e) => {
      const incBtn = e.target.closest("[data-qty-inc]");
      if (incBtn) {
        const id = incBtn.dataset.qtyInc;
        const current = store.cart.find((i) => i.productId === id);
        if (current) store.updateCartQuantity(id, current.quantity + 1);
        return;
      }

      const decBtn = e.target.closest("[data-qty-dec]");
      if (decBtn) {
        const id = decBtn.dataset.qtyDec;
        const current = store.cart.find((i) => i.productId === id);
        if (current) store.updateCartQuantity(id, current.quantity - 1);
        return;
      }

      const removeBtn = e.target.closest("[data-cart-remove]");
      if (removeBtn) {
        const id = removeBtn.dataset.cartRemove;
        const p = store.getProductById(id);
        store.removeFromCart(id);
        showToast(`Removed ${p ? p.name : "item"} from cart`, "info");
      }
    });

    // Checkout Flow
    DOM.checkoutBtn.addEventListener("click", openCheckoutModal);
    DOM.checkoutCloseBtn.addEventListener("click", () => DOM.checkoutModal.classList.remove("active"));

    DOM.checkoutForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const formData = new FormData(DOM.checkoutForm);
      const customer = {
        name: formData.get("name"),
        email: formData.get("email"),
        phone: formData.get("phone")
      };
      const shippingAddress = {
        street: formData.get("address"),
        city: formData.get("city"),
        zip: formData.get("zip")
      };
      const deliverySlot = formData.get("slot");
      const instructions = formData.get("instructions");

      const order = store.createOrder({
        customer,
        shippingAddress,
        deliverySlot,
        instructions
      });

      if (order) {
        DOM.checkoutModal.classList.remove("active");
        DOM.orderSuccessId.textContent = "#" + order.orderId;
        DOM.orderSuccessDelivery.textContent = order.deliverySlot;
        DOM.orderSuccessModal.classList.add("active");
        DOM.checkoutForm.reset();
        showToast("🎉 Order placed successfully!");
      }
    });

    DOM.orderSuccessCloseBtn.addEventListener("click", () => DOM.orderSuccessModal.classList.remove("active"));
    DOM.orderSuccessDoneBtn.addEventListener("click", () => DOM.orderSuccessModal.classList.remove("active"));

    // Admin Add Product Flow
    DOM.adminAddBtn.addEventListener("click", () => DOM.addProductModal.classList.add("active"));
    DOM.addProductCloseBtn.addEventListener("click", () => DOM.addProductModal.classList.remove("active"));

    DOM.addProductForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(DOM.addProductForm);
      const dietary = fd.getAll("dietary");

      const newProduct = store.addProduct({
        name: fd.get("name"),
        category: fd.get("category"),
        price: fd.get("price"),
        unit: fd.get("unit"),
        image: fd.get("image") || "🛒",
        stock: fd.get("stock"),
        origin: fd.get("origin"),
        description: fd.get("description"),
        badge: fd.get("badge"),
        dietary: dietary
      });

      DOM.addProductModal.classList.remove("active");
      DOM.addProductForm.reset();
      showToast(`Added product "${newProduct.name}"!`);
      renderProducts();
    });

    // Close modals on overlay backdrop click
    document.querySelectorAll(".modal-overlay").forEach((overlay) => {
      overlay.addEventListener("click", (e) => {
        if (e.target === overlay) {
          overlay.classList.remove("active");
        }
      });
    });

    // Listen to store updates
    store.subscribe((_, event) => {
      if (event === "products" || event === "favorites") {
        renderProducts();
        updateNavCounters();
      }
      if (event === "cart") {
        renderCart();
      }
      if (event === "orders") {
        updateNavCounters();
      }
    });
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // Initial Boot
  function init() {
    initElements();
    store.setTheme(store.theme);
    DOM.themeToggle.textContent = store.theme === "dark" ? "☀️" : "🌙";

    attachEvents();
    renderProducts();
    renderCart();
    updateNavCounters();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})(window);
