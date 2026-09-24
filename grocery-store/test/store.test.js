/**
 * Headless tests for the FreshCart store logic.
 * Run with: node grocery-store/test/store.test.js
 */
const fs = require("fs");
const path = require("path");
const vm = require("vm");

// --- Minimal browser environment stub ---
const storageData = {};
const localStorageStub = {
  getItem: (k) => (k in storageData ? storageData[k] : null),
  setItem: (k, v) => { storageData[k] = String(v); },
  removeItem: (k) => { delete storageData[k]; },
  clear: () => { for (const k of Object.keys(storageData)) delete storageData[k]; }
};

const sandbox = {
  console,
  localStorage: localStorageStub,
  document: { documentElement: { setAttribute() {} } },
  setTimeout,
  Math,
  Date,
  JSON
};
sandbox.window = sandbox;
vm.createContext(sandbox);

const base = path.join(__dirname, "..");
vm.runInContext(fs.readFileSync(path.join(base, "data/products-data.js"), "utf8"), sandbox);
vm.runInContext(fs.readFileSync(path.join(base, "js/store.js"), "utf8"), sandbox);

const store = sandbox.FreshStore;

// --- Tiny test harness ---
let passed = 0;
let failed = 0;
function assert(condition, message) {
  if (condition) {
    passed++;
    console.log("  \u2705 " + message);
  } else {
    failed++;
    console.error("  \u274c " + message);
  }
}
function section(name) {
  console.log("\n" + name);
}

// 1. Catalog loading
section("Catalog");
assert(store.products.length === 28, "loads 28 seed products (got " + store.products.length + ")");
assert(store.getProductById("prod_1").name === "Fresh Organic Bananas", "getProductById works");

// 2. Filtering
section("Filtering");
store.activeCategory = "dairy";
assert(store.getFilteredProducts().every((p) => p.category === "dairy"), "category filter returns only dairy");
const dairyCount = store.getFilteredProducts().length;
assert(dairyCount === 5, "dairy category has 5 items (got " + dairyCount + ")");
store.activeCategory = "all";

store.searchQuery = "coffee";
assert(store.getFilteredProducts().length === 1, "search 'coffee' returns 1 result");
store.searchQuery = "";

store.selectedDiet = "vegan";
assert(store.getFilteredProducts().every((p) => p.dietary.includes("vegan")), "dietary filter returns vegan items");
store.selectedDiet = "all";

store.priceRange = 2.5;
assert(store.getFilteredProducts().every((p) => p.price <= 2.5), "price filter respects max price");
store.priceRange = 25;

// 3. Sorting
section("Sorting");
store.sortBy = "price-low";
let list = store.getFilteredProducts();
assert(list[0].price <= list[list.length - 1].price, "sorts price ascending");
store.sortBy = "price-high";
list = store.getFilteredProducts();
assert(list[0].price >= list[list.length - 1].price, "sorts price descending");
store.sortBy = "rating";
list = store.getFilteredProducts();
assert(list[0].rating >= list[list.length - 1].rating, "sorts by rating");
store.sortBy = "name";
list = store.getFilteredProducts();
assert(list[0].name.localeCompare(list[1].name) <= 0, "sorts alphabetically");
store.sortBy = "featured";

// 4. Cart operations
section("Cart");
let res = store.addToCart("prod_1", 2);
assert(res.success === true, "addToCart succeeds");
assert(store.getCartTotalCount() === 2, "cart count is 2");
store.addToCart("prod_1", 1);
assert(store.cart.find((i) => i.productId === "prod_1").quantity === 3, "adding same product merges quantity");
store.updateCartQuantity("prod_1", 5);
assert(store.cart.find((i) => i.productId === "prod_1").quantity === 5, "updateCartQuantity works");
res = store.addToCart("prod_1", 9999);
assert(res.success === false && res.reason === "exceeds_stock", "cannot exceed available stock");
store.removeFromCart("prod_1");
assert(store.getCartTotalCount() === 0, "removeFromCart empties cart");

// 5. Pricing calculation
section("Pricing");
store.clearCart();
store.addToCart("prod_1", 2); // 1.99 * 2 = 3.98
let calc = store.getCartCalculations();
assert(Math.abs(calc.subtotal - 3.98) < 0.001, "subtotal computed correctly (" + calc.subtotal.toFixed(2) + ")");
assert(calc.deliveryFee === 4.99, "delivery fee applied under $35");
const expectedTax = 3.98 * 0.0825;
assert(Math.abs(calc.tax - expectedTax) < 0.001, "tax computed at 8.25%");
assert(Math.abs(calc.total - (3.98 + expectedTax + 4.99)) < 0.001, "total = subtotal + tax + delivery");

// Free delivery threshold
store.clearCart();
store.addToCart("prod_16", 3); // 12.99*3 = 38.97 > 35
calc = store.getCartCalculations();
assert(calc.subtotal > 35, "large cart exceeds $35");
assert(calc.deliveryFee === 0, "free delivery over $35");
assert(calc.amountForFreeDelivery === 0, "no amount needed for free delivery");

// 6. Coupons
section("Coupons");
let couponRes = store.applyCoupon("FRESH10");
assert(couponRes.success === true, "FRESH10 applies on $38.97 order");
calc = store.getCartCalculations();
assert(Math.abs(calc.discount - 3.897) < 0.001, "10% discount computed (" + calc.discount.toFixed(2) + ")");

couponRes = store.applyCoupon("BOGUS99");
assert(couponRes.success === false, "invalid coupon rejected");

store.clearCart();
store.addToCart("prod_1", 1); // 1.99
couponRes = store.applyCoupon("FRESH10");
assert(couponRes.success === false, "coupon blocked below minimum spend");

// SAVE20 requires $50 minimum - build a larger cart
store.removeCoupon();
store.addToCart("prod_16", 4); // 1.99 + 4*12.99 = 53.95
couponRes = store.applyCoupon("save20");
assert(couponRes.success === true, "SAVE20 applies over $50 minimum");
calc = store.getCartCalculations();
assert(Math.abs(calc.discount - calc.subtotal * 0.2) < 0.001, "SAVE20 gives 20% off");

// lower/upper-case normalization
store.removeCoupon();
couponRes = store.applyCoupon("  fresh10  ");
assert(couponRes.success === true, "lowercase + padded coupon code normalized");

// 7. Favorites
section("Favorites");
store.toggleFavorite("prod_5");
assert(store.isFavorite("prod_5") === true, "toggleFavorite adds favorite");
store.favoritesOnly = true;
assert(store.getFilteredProducts().length === 1, "favoritesOnly filter returns 1 item");
store.favoritesOnly = false;
store.toggleFavorite("prod_5");
assert(store.isFavorite("prod_5") === false, "toggleFavorite removes favorite");

// 8. Orders + stock decrement
section("Orders");
store.clearCart();
const stockBefore = store.getProductById("prod_7").stock; // eggs, stock 35
store.addToCart("prod_7", 2);
const order = store.createOrder({
  customer: { name: "Test User", email: "t@t.com", phone: "555" },
  shippingAddress: { street: "1 Main", city: "Town", zip: "12345" }
});
assert(order && order.orderId.startsWith("FC-"), "order created with FC- id");
assert(order.items.length === 1 && order.items[0].quantity === 2, "order snapshot captures items");
assert(store.getProductById("prod_7").stock === stockBefore - 2, "stock decremented after order");
assert(store.getCartTotalCount() === 0, "cart cleared after order");
assert(store.orders.length === 1, "order stored in history");
assert(order.pricing.total > 0, "order has positive total");

// 9. Persistence
section("Persistence");
assert(localStorageStub.getItem("freshcart_orders_v1") !== null, "orders persisted to localStorage");
assert(localStorageStub.getItem("freshcart_cart_v1") !== null, "cart persisted to localStorage");

// 10. Add / update / delete product
section("Product management");
const before = store.products.length;
const added = store.addProduct({ name: "Test Kiwi", category: "produce", price: "2.50", unit: "each", stock: "7" });
assert(store.products.length === before + 1, "addProduct adds item");
assert(store.getProductById(added.id).price === 2.5, "addProduct coerces price to number");
store.updateProduct(added.id, { price: 3.75 });
assert(store.getProductById(added.id).price === 3.75, "updateProduct updates price");
store.deleteProduct(added.id);
assert(store.getProductById(added.id) === undefined, "deleteProduct removes item");

// Summary
console.log("\n" + "=".repeat(40));
console.log("Passed: " + passed + "  Failed: " + failed);
console.log("=".repeat(40));
process.exit(failed === 0 ? 0 : 1);
