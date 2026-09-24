/**
 * FreshCart State & Store Manager
 */
(function (global) {
  "use strict";

  const STORAGE_KEYS = {
    PRODUCTS: "freshcart_products_v1",
    CART: "freshcart_cart_v1",
    ORDERS: "freshcart_orders_v1",
    FAVORITES: "freshcart_favs_v1",
    COUPON: "freshcart_coupon_v1",
    THEME: "freshcart_theme_v1"
  };

  const COUPONS = {
    FRESH10: { code: "FRESH10", discountPercent: 10, minSpend: 20, desc: "10% off orders over $20" },
    SAVE20: { code: "SAVE20", discountPercent: 20, minSpend: 50, desc: "20% off orders over $50" },
    SUPER5: { code: "SUPER5", discountAmount: 5, minSpend: 25, desc: "$5 off orders over $25" }
  };

  /* localStorage is unavailable in some contexts (file:// pages, private mode).
   * Fall back to an in-memory store so the app keeps working. */
  const memoryStore = {};
  const safeStorage = {
    getItem(key) {
      try {
        return global.localStorage.getItem(key);
      } catch (e) {
        return key in memoryStore ? memoryStore[key] : null;
      }
    },
    setItem(key, value) {
      try {
        global.localStorage.setItem(key, value);
      } catch (e) {
        memoryStore[key] = String(value);
      }
    },
    removeItem(key) {
      try {
        global.localStorage.removeItem(key);
      } catch (e) {
        delete memoryStore[key];
      }
    }
  };

  class Store {
    constructor() {
      this.listeners = new Set();
      this.products = this.loadProducts();
      this.cart = this.loadCart();
      this.orders = this.loadOrders();
      this.favorites = this.loadFavorites();
      this.appliedCoupon = this.loadCoupon();
      this.theme = safeStorage.getItem(STORAGE_KEYS.THEME) || "light";

      // Filter & Sort State
      this.activeCategory = "all";
      this.searchQuery = "";
      this.selectedDiet = "all";
      this.sortBy = "featured"; // featured, price-low, price-high, rating, name
      this.inStockOnly = false;
      this.priceRange = 25; // max price filter
      this.favoritesOnly = false;
    }

    subscribe(fn) {
      this.listeners.add(fn);
      return () => this.listeners.delete(fn);
    }

    notify(event = "change") {
      this.listeners.forEach((fn) => fn(this, event));
    }

    // Products
    loadProducts() {
      try {
        const stored = safeStorage.getItem(STORAGE_KEYS.PRODUCTS);
        if (stored) return JSON.parse(stored);
      } catch (e) {
        console.warn("Could not read products from localStorage", e);
      }
      return Array.isArray(global.INITIAL_PRODUCTS) ? [...global.INITIAL_PRODUCTS] : [];
    }

    saveProducts() {
      try {
        safeStorage.setItem(STORAGE_KEYS.PRODUCTS, JSON.stringify(this.products));
      } catch (e) {
        console.error("Failed to save products", e);
      }
      this.notify("products");
    }

    resetProducts() {
      this.products = Array.isArray(global.INITIAL_PRODUCTS) ? [...global.INITIAL_PRODUCTS] : [];
      this.saveProducts();
    }

    getProductById(id) {
      return this.products.find((p) => p.id === id);
    }

    addProduct(product) {
      const newProd = {
        id: "prod_" + Date.now(),
        name: product.name.trim(),
        category: product.category,
        price: parseFloat(product.price) || 0,
        unit: product.unit || "each",
        image: product.image || "🛒",
        description: product.description || "",
        rating: 5.0,
        reviewsCount: 1,
        stock: parseInt(product.stock, 10) || 10,
        origin: product.origin || "Local Farm",
        dietary: product.dietary || [],
        badge: product.badge || "New Item"
      };
      this.products.unshift(newProd);
      this.saveProducts();
      return newProd;
    }

    updateProduct(id, updates) {
      const idx = this.products.findIndex((p) => p.id === id);
      if (idx !== -1) {
        this.products[idx] = { ...this.products[idx], ...updates };
        this.saveProducts();
      }
    }

    deleteProduct(id) {
      this.products = this.products.filter((p) => p.id !== id);
      this.removeFromCart(id);
      this.saveProducts();
    }

    // Cart
    loadCart() {
      try {
        const stored = safeStorage.getItem(STORAGE_KEYS.CART);
        return stored ? JSON.parse(stored) : [];
      } catch (e) {
        return [];
      }
    }

    saveCart() {
      try {
        safeStorage.setItem(STORAGE_KEYS.CART, JSON.stringify(this.cart));
      } catch (e) {
        console.error("Failed to save cart", e);
      }
      this.notify("cart");
    }

    addToCart(productId, quantity = 1) {
      const product = this.getProductById(productId);
      if (!product || product.stock <= 0) return { success: false, reason: "out_of_stock" };

      const existing = this.cart.find((item) => item.productId === productId);
      const currentQty = existing ? existing.quantity : 0;
      const targetQty = currentQty + quantity;

      if (targetQty > product.stock) {
        return { success: false, reason: "exceeds_stock", max: product.stock };
      }

      if (existing) {
        existing.quantity = targetQty;
      } else {
        this.cart.push({ productId, quantity });
      }

      this.saveCart();
      return { success: true, count: this.getCartTotalCount() };
    }

    updateCartQuantity(productId, quantity) {
      const product = this.getProductById(productId);
      let qty = parseInt(quantity, 10);

      if (isNaN(qty) || qty <= 0) {
        this.removeFromCart(productId);
        return;
      }

      if (product && qty > product.stock) {
        qty = product.stock;
      }

      const existing = this.cart.find((item) => item.productId === productId);
      if (existing) {
        existing.quantity = qty;
        this.saveCart();
      }
    }

    removeFromCart(productId) {
      this.cart = this.cart.filter((item) => item.productId !== productId);
      this.saveCart();
    }

    clearCart() {
      this.cart = [];
      this.appliedCoupon = null;
      safeStorage.removeItem(STORAGE_KEYS.COUPON);
      this.saveCart();
    }

    getCartTotalCount() {
      return this.cart.reduce((sum, item) => sum + item.quantity, 0);
    }

    getCartCalculations() {
      let subtotal = 0;
      const items = [];

      this.cart.forEach((item) => {
        const product = this.getProductById(item.productId);
        if (product) {
          const lineTotal = product.price * item.quantity;
          subtotal += lineTotal;
          items.push({
            ...item,
            product,
            lineTotal
          });
        }
      });

      const deliveryFee = subtotal >= 35 || subtotal === 0 ? 0 : 4.99;
      const taxRate = 0.0825; // 8.25%
      const tax = subtotal * taxRate;

      let discount = 0;
      if (this.appliedCoupon) {
        const coupon = COUPONS[this.appliedCoupon];
        if (coupon && subtotal >= (coupon.minSpend || 0)) {
          if (coupon.discountPercent) {
            discount = (subtotal * coupon.discountPercent) / 100;
          } else if (coupon.discountAmount) {
            discount = Math.min(coupon.discountAmount, subtotal);
          }
        }
      }

      const total = Math.max(0, subtotal - discount + tax + (subtotal > 0 ? deliveryFee : 0));

      return {
        items,
        subtotal,
        discount,
        tax,
        deliveryFee,
        total,
        freeDeliveryProgress: Math.min(100, (subtotal / 35) * 100),
        amountForFreeDelivery: Math.max(0, 35 - subtotal),
        coupon: this.appliedCoupon ? COUPONS[this.appliedCoupon] : null
      };
    }

    // Coupons
    loadCoupon() {
      try {
        return safeStorage.getItem(STORAGE_KEYS.COUPON) || null;
      } catch (e) {
        return null;
      }
    }

    applyCoupon(code) {
      const cleanCode = (code || "").trim().toUpperCase();
      const coupon = COUPONS[cleanCode];
      if (!coupon) {
        return { success: false, message: "Invalid promo code" };
      }

      const { subtotal } = this.getCartCalculations();
      if (subtotal < coupon.minSpend) {
        return {
          success: false,
          message: "Minimum spend of $" + coupon.minSpend.toFixed(2) + " required for code " + cleanCode
        };
      }

      this.appliedCoupon = cleanCode;
      try {
        safeStorage.setItem(STORAGE_KEYS.COUPON, cleanCode);
      } catch (e) {}
      this.notify("cart");
      return { success: true, coupon };
    }

    removeCoupon() {
      this.appliedCoupon = null;
      try {
        safeStorage.removeItem(STORAGE_KEYS.COUPON);
      } catch (e) {}
      this.notify("cart");
    }

    // Favorites
    loadFavorites() {
      try {
        const stored = safeStorage.getItem(STORAGE_KEYS.FAVORITES);
        return stored ? JSON.parse(stored) : [];
      } catch (e) {
        return [];
      }
    }

    toggleFavorite(productId) {
      if (this.favorites.includes(productId)) {
        this.favorites = this.favorites.filter((id) => id !== productId);
      } else {
        this.favorites.push(productId);
      }
      try {
        safeStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify(this.favorites));
      } catch (e) {}
      this.notify("favorites");
      return this.favorites.includes(productId);
    }

    isFavorite(productId) {
      return this.favorites.includes(productId);
    }

    // Orders
    loadOrders() {
      try {
        const stored = safeStorage.getItem(STORAGE_KEYS.ORDERS);
        return stored ? JSON.parse(stored) : [];
      } catch (e) {
        return [];
      }
    }

    createOrder(orderDetails) {
      const calc = this.getCartCalculations();
      if (calc.items.length === 0) return null;

      const order = {
        orderId: "FC-" + Math.floor(100000 + Math.random() * 900000),
        placedAt: new Date().toISOString(),
        customer: orderDetails.customer,
        shippingAddress: orderDetails.shippingAddress,
        deliverySlot: orderDetails.deliverySlot || "Standard Next-Day (8am - 12pm)",
        paymentMethod: orderDetails.paymentMethod || "Credit Card (ending 4242)",
        instructions: orderDetails.instructions || "",
        items: calc.items.map((it) => ({
          productId: it.productId,
          name: it.product.name,
          image: it.product.image,
          price: it.product.price,
          unit: it.product.unit,
          quantity: it.quantity,
          lineTotal: it.lineTotal
        })),
        pricing: {
          subtotal: calc.subtotal,
          discount: calc.discount,
          tax: calc.tax,
          deliveryFee: calc.deliveryFee,
          total: calc.total,
          coupon: calc.coupon ? calc.coupon.code : null
        },
        status: "Processing",
        estimatedDelivery: new Date(Date.now() + 2 * 3600 * 1000).toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit"
        })
      };

      // Decrement product stock
      calc.items.forEach((it) => {
        const p = this.getProductById(it.productId);
        if (p) {
          p.stock = Math.max(0, p.stock - it.quantity);
        }
      });
      this.saveProducts();

      this.orders.unshift(order);
      try {
        safeStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(this.orders));
      } catch (e) {}

      this.clearCart();
      this.notify("orders");
      return order;
    }

    // Filter & Search
    getFilteredProducts() {
      return this.products
        .filter((product) => {
          if (this.favoritesOnly && !this.isFavorite(product.id)) {
            return false;
          }

          if (this.activeCategory !== "all" && product.category !== this.activeCategory) {
            return false;
          }

          if (this.searchQuery && this.searchQuery.trim()) {
            const q = this.searchQuery.toLowerCase().trim();
            const matchName = product.name.toLowerCase().includes(q);
            const matchDesc = product.description.toLowerCase().includes(q);
            const matchOrigin = (product.origin || "").toLowerCase().includes(q);
            const matchCat = product.category.toLowerCase().includes(q);
            if (!matchName && !matchDesc && !matchOrigin && !matchCat) {
              return false;
            }
          }

          if (this.selectedDiet !== "all") {
            if (!product.dietary || !product.dietary.includes(this.selectedDiet)) {
              return false;
            }
          }

          if (product.price > this.priceRange) {
            return false;
          }

          if (this.inStockOnly && product.stock <= 0) {
            return false;
          }

          return true;
        })
        .sort((a, b) => {
          switch (this.sortBy) {
            case "price-low":
              return a.price - b.price;
            case "price-high":
              return b.price - a.price;
            case "rating":
              return b.rating - a.rating;
            case "name":
              return a.name.localeCompare(b.name);
            case "featured":
            default:
              return (b.badge ? 1 : 0) - (a.badge ? 1 : 0);
          }
        });
    }

    setTheme(theme) {
      this.theme = theme;
      safeStorage.setItem(STORAGE_KEYS.THEME, theme);
      document.documentElement.setAttribute("data-theme", theme);
      this.notify("theme");
    }
  }

  global.FreshStore = new Store();
  global.FreshStoreCOUPONS = COUPONS;
})(window);
