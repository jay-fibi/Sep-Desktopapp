/**
 * Regenerates data/products-data.js from data/products.json.
 *
 * products.json is the human-editable source of truth. Because browsers block
 * loading a local JSON file over the file:// protocol, the same catalog is
 * emitted as a plain JS file that assigns window.INITIAL_PRODUCTS. Keeping both
 * lets the app work when the folder is opened directly (no web server).
 *
 * Usage: node scripts/build-data.js
 */
const fs = require("fs");
const path = require("path");

const dataDir = path.join(__dirname, "..", "data");
const jsonPath = path.join(dataDir, "products.json");
const outPath = path.join(dataDir, "products-data.js");

const products = JSON.parse(fs.readFileSync(jsonPath, "utf8"));

if (!Array.isArray(products)) {
  throw new Error("products.json must contain a JSON array");
}

const ids = new Set();
for (const p of products) {
  for (const field of ["id", "name", "category", "price", "unit"]) {
    if (p[field] === undefined) {
      throw new Error(`Product ${p.id || "(no id)"} is missing required field "${field}"`);
    }
  }
  if (ids.has(p.id)) {
    throw new Error(`Duplicate product id: ${p.id}`);
  }
  ids.add(p.id);
}

const banner =
  "/* AUTO-GENERATED from products.json - do not edit by hand.\n" +
  " * Regenerate with: node scripts/build-data.js */\n";

fs.writeFileSync(
  outPath,
  banner + "window.INITIAL_PRODUCTS = " + JSON.stringify(products, null, 2) + ";\n"
);

console.log(`Wrote ${products.length} products to ${path.relative(process.cwd(), outPath)}`);
