# 🥬 FreshCart

A fully functional, dependency-free grocery store web application — browse aisles,
search, filter, build a cart, apply promo codes, and check out with a simulated
payment flow. Everything runs in the browser with no build step and no backend.

## Features

- **28-item starter catalog** across 7 aisles: produce, dairy & eggs, artisan
  bakery, meat & seafood, pantry, beverages, and snacks.
- **Browse & discover** — hero banner, sticky category strip, and responsive
  product grid.
- **Search** by name, description, origin, or category.
- **Filter** by category, price ceiling, dietary labels (organic / vegan /
  vegetarian / gluten-free), and in-stock-only.
- **Sort** by featured, price (asc/desc), rating, or name.
- **Product detail modal** with rating, origin, dietary traits, and stock.
- **Favorites / wishlist** with a dedicated filtered view.
- **Shopping cart drawer** with quantity steppers, per-line totals, and a live
  free-delivery progress meter.
- **Promo codes** — `FRESH10` (10% off $20+), `SAVE20` (20% off $50+),
  `SUPER5` ($5 off $25+).
- **Checkout** with contact + delivery address, delivery window selection, and a
  simulated payment step, followed by an order confirmation with a generated
  order id.
- **Order history** with itemized receipts and totals.
- **Inventory management** — add new grocery items from the UI; stock is
  decremented automatically when orders are placed.
- **Persistence** via `localStorage` so the cart, orders, favorites, and custom
  products survive reloads (with a graceful in-memory fallback when storage is
  unavailable, e.g. on `file://`).
- **Light / dark theme** toggle, fully responsive down to mobile.

## Project structure

```
grocery-store/
├── index.html              # Single-page shell (markup for the whole app)
├── css/styles.css          # Design system, layout, and responsive rules
├── js/
│   ├── store.js            # State layer: catalog, cart, coupons, orders, filters
│   └── app.js              # UI controller: rendering + event wiring
├── data/
│   ├── products.json       # Editable source-of-truth catalog
│   └── products-data.js    # Generated browser-loadable catalog
├── scripts/
│   ├── build-data.js       # Regenerates products-data.js from products.json
│   └── serve.js            # Zero-dependency static server
└── test/
    ├── store.test.js       # Headless unit tests for the state layer
    └── ui.test.js          # DOM integration tests (jsdom)
```

## Running the app

No build step is required. Either open the file directly:

```bash
open grocery-store/index.html      # macOS
xdg-open grocery-store/index.html  # Linux
```

…or serve it (recommended, so `fetch`/`localStorage` behave like production):

```bash
node grocery-store/scripts/serve.js 8080
# then visit http://localhost:8080
```

## Editing the catalog

Edit `data/products.json`, then regenerate the browser file:

```bash
node scripts/build-data.js
```

## Tests

State-layer tests (no dependencies):

```bash
node test/store.test.js
```

DOM/UI integration tests use [jsdom](https://github.com/jsdom/jsdom):

```bash
npm install          # installs jsdom
node test/ui.test.js
```

## Promo codes

| Code      | Discount          | Minimum spend |
| --------- | ----------------- | ------------- |
| `FRESH10` | 10% off           | $20           |
| `SAVE20`  | 20% off           | $50           |
| `SUPER5`  | $5 off            | $25           |

Free doorstep delivery applies automatically on carts that reach **$35**.
