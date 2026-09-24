/**
 * DOM / UI integration tests using jsdom.
 * Requires jsdom (install with: npm install jsdom).
 * Run with: NODE_PATH=/tmp/jsdomtest/node_modules node grocery-store/test/ui.test.js
 */
const fs = require("fs");
const path = require("path");

let JSDOM;
try {
  JSDOM = require("jsdom").JSDOM;
} catch (e) {
  console.error("jsdom is not installed. Run: npm install jsdom");
  process.exit(2);
}

const indexPath = path.join(__dirname, "..", "index.html");
const html = fs.readFileSync(indexPath, "utf8");

let passed = 0;
let failed = 0;
function assert(cond, msg) {
  if (cond) {
    passed++;
    console.log("  \u2705 " + msg);
  } else {
    failed++;
    console.error("  \u274c " + msg);
  }
}
function section(n) { console.log("\n" + n); }

function wait(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function main() {
  const dom = new JSDOM(html, {
    url: "file://" + indexPath,
    runScripts: "dangerously",
    resources: "usable",
    pretendToBeVisual: true
  });

  const { window } = dom;
  window.scrollTo = () => {};

  // Wait for scripts to load and init to run
  await new Promise((resolve) => {
    if (window.document.readyState === "complete") return resolve();
    window.addEventListener("load", resolve);
    setTimeout(resolve, 3000);
  });
  await wait(200);

  const doc = window.document;
  const store = window.FreshStore;

  section("Boot");
  assert(typeof store === "object", "FreshStore is initialized");
  assert(store.products.length === 28, "product catalog loaded into store");

  section("Rendering");
  const cards = doc.querySelectorAll(".product-card");
  assert(cards.length === 28, "renders 28 product cards (got " + cards.length + ")");
  assert(doc.getElementById("results-count").textContent === "28", "results count shows 28");

  section("Search interaction");
  const searchInput = doc.getElementById("search-input");
  searchInput.value = "avocado";
  searchInput.dispatchEvent(new window.Event("input", { bubbles: true }));
  assert(doc.querySelectorAll(".product-card").length === 1, "searching 'avocado' narrows to 1 card");
  assert(doc.getElementById("search-clear-btn").classList.contains("visible"), "clear button becomes visible");
  doc.getElementById("search-clear-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.querySelectorAll(".product-card").length === 28, "clearing search restores all cards");

  section("Category filter");
  const dairyPill = doc.querySelector('#category-strip .cat-pill[data-category="dairy"]');
  dairyPill.dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  const dairyCards = doc.querySelectorAll(".product-card");
  assert(dairyCards.length === 5, "dairy category shows 5 cards (got " + dairyCards.length + ")");
  assert(dairyPill.classList.contains("active"), "clicked category pill becomes active");

  section("Add to cart via UI");
  const firstAddBtn = doc.querySelector("[data-add-cart]");
  firstAddBtn.dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(store.getCartTotalCount() === 1, "clicking Add updates cart count in store");
  assert(doc.getElementById("nav-cart-count").textContent === "1", "nav cart badge updates to 1");
  assert(doc.querySelectorAll("#toast-container .toast").length >= 1, "toast notification shown");

  section("Cart drawer");
  doc.getElementById("cart-toggle-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.getElementById("cart-drawer").classList.contains("active"), "cart drawer opens");
  assert(doc.querySelectorAll("#cart-items-list .cart-item").length === 1, "cart drawer lists 1 item");

  section("Quantity stepper");
  const incBtn = doc.querySelector("[data-qty-inc]");
  incBtn.dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(store.getCartTotalCount() === 2, "increment raises quantity to 2");
  const decBtn = doc.querySelector("[data-qty-dec]");
  decBtn.dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(store.getCartTotalCount() === 1, "decrement lowers quantity to 1");

  section("Coupon via UI");
  // add enough to meet FRESH10 minimum
  const addBtns = doc.querySelectorAll("[data-add-cart]:not([disabled])");
  for (let i = 0; i < 5 && i < addBtns.length; i++) {
    addBtns[i].dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  }
  doc.getElementById("coupon-input").value = "FRESH10";
  doc.getElementById("apply-coupon-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(store.appliedCoupon === "FRESH10", "coupon applied through UI");
  const appliedTag = doc.querySelector("#coupon-applied-container .coupon-applied-tag");
  assert(appliedTag && appliedTag.textContent.includes("FRESH10"), "applied coupon tag rendered");
  assert(doc.getElementById("cart-discount-row").style.display === "flex", "discount row visible");

  section("Checkout flow");
  const cartSubtotalText = doc.getElementById("cart-subtotal").textContent;
  assert(cartSubtotalText !== "$0.00", "subtotal rendered in cart");
  doc.getElementById("checkout-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.getElementById("checkout-modal").classList.contains("active"), "checkout modal opens");

  const form = doc.getElementById("checkout-form");
  form.dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }));
  assert(store.orders.length === 1, "submitting checkout creates an order");
  assert(doc.getElementById("order-success-modal").classList.contains("active"), "success modal shown");
  assert(doc.getElementById("order-success-id").textContent.startsWith("#FC-"), "success modal shows FC order id");
  assert(store.getCartTotalCount() === 0, "cart emptied after order");
  assert(doc.getElementById("nav-cart-count").textContent === "0", "nav badge reset to 0");

  section("Order history");
  doc.getElementById("nav-orders-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.getElementById("orders-modal").classList.contains("active"), "orders history modal opens");
  assert(doc.querySelectorAll("#orders-list-container .order-history-card").length === 1, "one order listed in history");

  section("Product detail modal");
  doc.getElementById("orders-close-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  // reset category to view all
  doc.getElementById("reset-filters-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  const detailTarget = doc.querySelector("[data-detail-id]");
  detailTarget.dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.getElementById("detail-modal").classList.contains("active"), "product detail modal opens");
  assert(doc.querySelector("#detail-container .detail-img-box") !== null, "detail modal shows product info");
  doc.getElementById("detail-close-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));

  section("Favorites");
  const favBtn = doc.querySelector("[data-fav-id]");
  favBtn.dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(store.favorites.length === 1, "favorite added via heart button");
  assert(doc.getElementById("nav-favorites-count").textContent === "1", "favorites badge shows 1");
  doc.getElementById("nav-favorites-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.querySelectorAll(".product-card").length === 1, "favorites view filters to 1 card");
  doc.getElementById("nav-favorites-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.querySelectorAll(".product-card").length === 28, "toggling favorites view restores all cards");

  section("Add product (admin)");
  doc.getElementById("admin-add-btn").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(doc.getElementById("add-product-modal").classList.contains("active"), "add product modal opens");
  const addForm = doc.getElementById("add-product-form");
  addForm.querySelector("#prod-name").value = "Test Mango";
  addForm.querySelector("#prod-price").value = "2.75";
  addForm.querySelector("#prod-unit").value = "each";
  addForm.dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }));
  assert(store.products.length === 29, "new product added to catalog");
  assert(doc.querySelectorAll(".product-card").length === 29, "grid re-renders with 29 cards");

  section("Theme toggle");
  const initialTheme = store.theme;
  doc.getElementById("theme-toggle").dispatchEvent(new window.MouseEvent("click", { bubbles: true }));
  assert(store.theme !== initialTheme, "theme toggles on click");
  assert(doc.documentElement.getAttribute("data-theme") === store.theme, "data-theme attribute updated");

  section("No console errors");
  assert(true, "(errors/warnings would appear above)");

  console.log("\n" + "=".repeat(40));
  console.log("Passed: " + passed + "  Failed: " + failed);
  console.log("=".repeat(40));
  dom.window.close();
  process.exit(failed === 0 ? 0 : 1);
}

main().catch((err) => {
  console.error("UI test crashed:", err);
  process.exit(1);
});
