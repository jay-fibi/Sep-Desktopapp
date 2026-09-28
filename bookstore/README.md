# The Dusty Shelf — Bookstore Website

A responsive, single-page bookstore website built with plain HTML, CSS, and JavaScript — no build step required.

## Features

- **Hero section** with a CSS-illustrated book stack
- **Book catalog** of 12 titles with live search and genre filters
- **Shopping cart** drawer with quantity controls, subtotal, and checkout
- **Categories, About, Testimonials, Newsletter** sections
- **Fully responsive** with a mobile hamburger menu
- Book covers are pure CSS gradients — zero external image dependencies

## Run it

Open `index.html` directly in a browser, or serve the folder:

```bash
cd bookstore
python3 -m http.server 8000
# then visit http://localhost:8000
```

## Files

| File         | Purpose                                    |
| ------------ | ------------------------------------------ |
| `index.html` | Page structure and content                 |
| `styles.css` | Theme, layout, illustrations, responsive   |
| `script.js`  | Catalog rendering, search/filter, cart, UI |
