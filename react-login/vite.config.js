import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

/* Plain client-side React app: no API, no server-side rendering.
 * `test` is the Vitest block used by `npm test`. */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: false
  },
  test: {
    environment: "jsdom",
    globals: true,
    include: ["src/**/*.test.{js,jsx}"],
    setupFiles: ["./src/test/setup.js"]
  }
});
