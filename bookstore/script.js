/* ===== The Dusty Shelf — script.js ===== */

// ---------- Book catalog ----------
const BOOKS = [
  { id: 1,  title: "The Night Orchard",      author: "Elena Marlowe",    genre: "fiction",    price: 18.99, rating: 4.8, badge: "Bestseller", cover: ["#2f5241", "#4a7a5f"] },
  { id: 2,  title: "Salt & Cinder",          author: "Rowan Ashe",       genre: "romance",    price: 15.50, rating: 4.6, badge: null,          cover: ["#c2571f", "#e08a55"] },
  { id: 3,  title: "The Clock Garden",       author: "Iris Fenwick",     genre: "mystery",    price: 16.99, rating: 4.7, badge: "Staff Pick", cover: ["#31435e", "#5a7196"] },
  { id: 4,  title: "Atlas of Small Towns",   author: "Theo Brandt",      genre: "nonfiction", price: 24.00, rating: 4.9, badge: null,          cover: ["#7a4a2b", "#a9713f"] },
  { id: 5,  title: "A Sky of Static",        author: "June Okafor",      genre: "scifi",      price: 19.99, rating: 4.5, badge: null,          cover: ["#4b2e5e", "#7d5499"] },
  { id: 6,  title: "The Silent Cartographer",author: "Marcus Vale",      genre: "mystery",    price: 17.50, rating: 4.4, badge: null,          cover: ["#5e3a3a", "#966060"] },
  { id: 7,  title: "Letters from Juniper St.",author: "Ada Limón-Grey",  genre: "romance",    price: 14.99, rating: 4.7, badge: "New",        cover: ["#b0475e", "#d97a8e"] },
  { id: 8,  title: "The Last Library Ship",  author: "Kenji Sato",       genre: "scifi",      price: 21.00, rating: 4.8, badge: "Staff Pick", cover: ["#1f4e5e", "#3f8196"] },
  { id: 9,  title: "Bread, Salt, Roses",     author: "Carmen Ruiz",      genre: "fiction",    price: 16.50, rating: 4.6, badge: null,          cover: ["#8a6d2b", "#c2a04a"] },
  { id: 10, title: "How Mountains Sleep",    author: "Ingrid Halvorsen", genre: "nonfiction", price: 22.99, rating: 4.9, badge: "Bestseller", cover: ["#3d5a45", "#6e9678"] },
  { id: 11, title: "The Widow's Cipher",     author: "Nora Blackwood",   genre: "mystery",    price: 15.99, rating: 4.3, badge: null,          cover: ["#2b2b3d", "#55557a"] },
  { id: 12, title: "Gravity of Us",          author: "Felix Anders",     genre: "romance",    price: 14.50, rating: 4.2, badge: null,          cover: ["#a34d2b", "#d97e55"] },
];

const GENRE_LABELS = {
  fiction: "Fiction", mystery: "Mystery", scifi: "Sci-Fi",
  romance: "Romance", nonfiction: "Non-Fiction",
};

const money = (n) => `$${n.toFixed(2)}`;
const stars = (r) => "★".repeat(Math.round(r)) + "☆".repeat(5 - Math.round(r));

// ---------- Render book grid ----------
const grid = document.getElementById("book-grid");
const noResults = document.getElementById("no-results");
const searchInput = document.getElementById("book-search");
const pills = document.querySelectorAll("#filter-pills .pill");

let activeGenre = "all";
let query = "";

function renderBooks() {
  const q = query.trim().toLowerCase();
  const visible = BOOKS.filter((b) => {
    const matchGenre = activeGenre === "all" || b.genre === activeGenre;
    const matchQuery =
      !q || b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q);
    return matchGenre && matchQuery;
  });

  grid.innerHTML = visible
    .map(
      (b) => `
      <article class="book-card" data-id="${b.id}">
        <div class="book-cover" style="background: linear-gradient(135deg, ${b.cover[0]}, ${b.cover[1]});">
          ${b.badge ? `<span class="book-badge">${b.badge}</span>` : ""}
          <span class="cover-title">${b.title}</span>
        </div>
        <div class="book-body">
          <h3 class="book-title">${b.title}</h3>
          <p class="book-author">by ${b.author} · ${GENRE_LABELS[b.genre]}</p>
          <div class="book-meta">
            <span class="book-price">${money(b.price)}</span>
            <span class="book-rating" title="${b.rating} out of 5">${stars(b.rating)} ${b.rating}</span>
          </div>
          <button class="add-btn" data-add="${b.id}">Add to Cart</button>
        </div>
      </article>`
    )
    .join("");

  noResults.hidden = visible.length > 0;
}

searchInput.addEventListener("input", (e) => {
  query = e.target.value;
  renderBooks();
});

pills.forEach((pill) => {
  pill.addEventListener("click", () => {
    pills.forEach((p) => p.classList.remove("active"));
    pill.classList.add("active");
    activeGenre = pill.dataset.filter;
    renderBooks();
  });
});

// Category cards jump to the grid with the genre pre-filtered
document.querySelectorAll(".category-card[data-genre]").forEach((card) => {
  card.addEventListener("click", () => {
    const genre = card.dataset.genre;
    const pill = document.querySelector(`#filter-pills .pill[data-filter="${genre}"]`);
    if (pill) pill.click();
  });
});

// ---------- Cart ----------
const cart = new Map(); // id -> qty

const cartDrawer = document.getElementById("cart-drawer");
const cartOverlay = document.getElementById("cart-overlay");
const cartItemsEl = document.getElementById("cart-items");
const cartEmptyEl = document.getElementById("cart-empty");
const cartCountEl = document.getElementById("cart-count");
const cartTotalEl = document.getElementById("cart-total");
const checkoutBtn = document.getElementById("checkout-btn");
const toast = document.getElementById("toast");

let toastTimer;
function showToast(msg) {
  toast.textContent = msg;
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2200);
}

function openCart() {
  cartOverlay.hidden = false;
  requestAnimationFrame(() => cartOverlay.classList.add("show"));
  cartDrawer.classList.add("open");
  cartDrawer.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
}

function closeCart() {
  cartOverlay.classList.remove("show");
  setTimeout(() => (cartOverlay.hidden = true), 250);
  cartDrawer.classList.remove("open");
  cartDrawer.setAttribute("aria-hidden", "true");
  document.body.style.overflow = "";
}

document.getElementById("cart-open").addEventListener("click", openCart);
document.getElementById("cart-close").addEventListener("click", closeCart);
cartOverlay.addEventListener("click", closeCart);
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && cartDrawer.classList.contains("open")) closeCart();
});

function cartTotals() {
  let count = 0, total = 0;
  for (const [id, qty] of cart) {
    const book = BOOKS.find((b) => b.id === id);
    count += qty;
    total += book.price * qty;
  }
  return { count, total };
}

function renderCart() {
  const { count, total } = cartTotals();
  cartCountEl.textContent = count;
  cartTotalEl.textContent = money(total);
  checkoutBtn.disabled = count === 0;
  cartEmptyEl.style.display = count === 0 ? "" : "none";

  cartItemsEl.querySelectorAll(".cart-item").forEach((el) => el.remove());

  for (const [id, qty] of cart) {
    const b = BOOKS.find((book) => book.id === id);
    const item = document.createElement("div");
    item.className = "cart-item";
    item.innerHTML = `
      <div class="cart-item-cover" style="background: linear-gradient(135deg, ${b.cover[0]}, ${b.cover[1]});">${b.title}</div>
      <div>
        <p class="cart-item-title">${b.title}</p>
        <p class="cart-item-price">${money(b.price)} each</p>
        <div class="qty-controls">
          <button data-dec="${id}" aria-label="Decrease quantity">−</button>
          <span>${qty}</span>
          <button data-inc="${id}" aria-label="Increase quantity">+</button>
        </div>
      </div>
      <button class="cart-item-remove" data-remove="${id}" aria-label="Remove ${b.title}">✕</button>`;
    cartItemsEl.appendChild(item);
  }
}

function addToCart(id) {
  cart.set(id, (cart.get(id) || 0) + 1);
  renderCart();
  const book = BOOKS.find((b) => b.id === id);
  showToast(`Added “${book.title}” to your cart`);
}

function addToCartSilent(id) {
  cart.set(id, (cart.get(id) || 0) + 1);
  renderCart();
}

// Delegate clicks for add-to-cart and quantity controls
document.addEventListener("click", (e) => {
  const add = e.target.closest("[data-add]");
  if (add) {
    addToCart(Number(add.dataset.add));
    add.classList.add("added");
    add.textContent = "Added ✓";
    setTimeout(() => { add.classList.remove("added"); add.textContent = "Add to Cart"; }, 1400);
    return;
  }
  const inc = e.target.closest("[data-inc]");
  if (inc) { addToCartSilent(Number(inc.dataset.inc)); return; }
  const dec = e.target.closest("[data-dec]");
  if (dec) {
    const id = Number(dec.dataset.dec);
    const qty = (cart.get(id) || 0) - 1;
    qty <= 0 ? cart.delete(id) : cart.set(id, qty);
    renderCart();
    return;
  }
  const rem = e.target.closest("[data-remove]");
  if (rem) {
    cart.delete(Number(rem.dataset.remove));
    renderCart();
  }
});

checkoutBtn.addEventListener("click", () => {
  const { count, total } = cartTotals();
  if (count === 0) return;
  showToast(`Order placed! ${count} book${count > 1 ? "s" : ""} · ${money(total)} — see you at pickup 📚`);
  cart.clear();
  renderCart();
  closeCart();
});

// ---------- Newsletter ----------
const form = document.getElementById("newsletter-form");
const msg = document.getElementById("newsletter-msg");
form.addEventListener("submit", (e) => {
  e.preventDefault();
  const email = document.getElementById("newsletter-email").value.trim();
  const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  msg.textContent = valid
    ? `Thanks! The next book list is headed to ${email}.`
    : "Please enter a valid email address.";
  if (valid) form.reset();
});

// ---------- Mobile menu ----------
const menuToggle = document.getElementById("menu-toggle");
const mainNav = document.getElementById("main-nav");
menuToggle.addEventListener("click", () => {
  const open = mainNav.classList.toggle("open");
  menuToggle.classList.toggle("open", open);
  menuToggle.setAttribute("aria-expanded", open);
});
mainNav.querySelectorAll("a").forEach((a) =>
  a.addEventListener("click", () => {
    mainNav.classList.remove("open");
    menuToggle.classList.remove("open");
    menuToggle.setAttribute("aria-expanded", "false");
  })
);

// ---------- Init ----------
document.getElementById("year").textContent = new Date().getFullYear();
renderBooks();
renderCart();

