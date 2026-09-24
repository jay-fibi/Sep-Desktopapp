/* Vitest setup: unmount the rendered tree and drop the demo session between
 * tests so nothing leaks from one case to the next. */
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

afterEach(() => {
  cleanup();
  window.localStorage.clear();
});
