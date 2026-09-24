/* Unit tests for the validation helpers (`npm test`). */

import { describe, expect, it } from "vitest";
import {
  credentialsMatch,
  emailError,
  isValidEmail,
  maskEmail,
  minPasswordLength,
  normalizeEmail,
  passwordError,
  validateCredentials
} from "./credentials.js";

const DEMO_USER = { email: "demo@example.com", password: "calculator123" };

describe("normalizeEmail", () => {
  it("trims and lower-cases", () => {
    expect(normalizeEmail("  Demo@Example.COM ")).toBe("demo@example.com");
    expect(normalizeEmail(null)).toBe("");
  });
});

describe("isValidEmail", () => {
  it("accepts plain addresses and rejects broken ones", () => {
    expect(isValidEmail("demo@example.com")).toBe(true);
    expect(isValidEmail(" a.b-c+d@sub.example.co ")).toBe(true);
    expect(isValidEmail("demo@example")).toBe(false);
    expect(isValidEmail("demo example.com")).toBe(false);
    expect(isValidEmail("")).toBe(false);
  });
});

describe("emailError", () => {
  it("returns a message only for bad input", () => {
    expect(emailError("demo@example.com")).toBe("");
    expect(emailError("")).toBe("Enter your email address");
    expect(emailError("nope")).toBe("'nope' is not a valid email address");
  });
});

describe("minPasswordLength", () => {
  it("falls back to 8 for nonsense limits", () => {
    expect(minPasswordLength(12)).toBe(12);
    expect(minPasswordLength(0)).toBe(8);
    expect(minPasswordLength("abc")).toBe(8);
  });
});

describe("passwordError", () => {
  it("enforces the minimum length", () => {
    expect(passwordError("calculator123", 8)).toBe("");
    expect(passwordError("", 8)).toBe("Enter your password");
    expect(passwordError("short", 8)).toBe("Password must be at least 8 characters");
  });
});

describe("validateCredentials", () => {
  it("reports both fields at once", () => {
    const bad = validateCredentials("nope", "short", 8);
    expect(bad.valid).toBe(false);
    expect(bad.errors.email).toBe("'nope' is not a valid email address");
    expect(bad.errors.password).toBe("Password must be at least 8 characters");

    const good = validateCredentials(" Demo@Example.com ", "calculator123", 8);
    expect(good.valid).toBe(true);
    expect(good.email).toBe("demo@example.com");
  });
});

describe("credentialsMatch", () => {
  it("compares against the demo user", () => {
    expect(credentialsMatch("DEMO@example.com", "calculator123", DEMO_USER)).toBe(true);
    expect(credentialsMatch("demo@example.com", "wrong-password", DEMO_USER)).toBe(false);
    expect(credentialsMatch("demo@example.com", "calculator123", null)).toBe(false);
  });
});

describe("maskEmail", () => {
  it("hides the middle of the local part", () => {
    expect(maskEmail("demo@example.com")).toBe("d***o@example.com");
    expect(maskEmail("a@example.com")).toBe("a***@example.com");
    expect(maskEmail("not-an-email")).toBe("not-an-email");
  });
});
