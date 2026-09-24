/* End-to-end-ish tests of the screen: render <App />, type like a user would
 * and check what the DOM and localStorage end up containing. */

import { describe, expect, it } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "./App.jsx";
import { authConfig } from "./config.js";

const TIMEOUT = { timeout: 4000 };

function typeCredentials({ email, password }) {
  if (email !== undefined) {
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
  }
  if (password !== undefined) {
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  }
}

function submitForm() {
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

describe("<App />", () => {
  it("renders the sign-in screen with an empty form", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: "Sign in" })).toBeDefined();
    expect(screen.getByLabelText("Email").value).toBe("");
    expect(screen.getByLabelText("Password").value).toBe("");
    expect(screen.getByText(/demo@example\.com \/ calculator123/)).toBeDefined();
  });

  it("shows validation messages instead of signing in", () => {
    render(<App />);
    submitForm();

    expect(document.getElementById("email-error").textContent).toBe("Enter your email address");
    expect(document.getElementById("password-error").textContent).toBe("Enter your password");
    expect(screen.getByRole("status").textContent).toBe("Enter your email address");

    typeCredentials({ email: "not-an-email" });
    expect(document.getElementById("email-error").textContent).toBe(
      "'not-an-email' is not a valid email address"
    );
  });

  it("rejects a wrong password and counts the attempt", async () => {
    render(<App />);
    typeCredentials({ email: "demo@example.com", password: "wrong-password" });
    submitForm();

    await waitFor(() => {
      expect(screen.getByRole("status").textContent).toBe("Wrong email or password (1/5)");
    }, TIMEOUT);
    expect(screen.getByLabelText("Password").value).toBe("");
    expect(window.localStorage.getItem(authConfig.sessionKey)).toBeNull();
  });

  it("signs in with the demo credentials and remembers the email", async () => {
    render(<App />);
    fireEvent.click(screen.getByRole("checkbox", { name: "Remember my email" }));
    fireEvent.click(screen.getByRole("button", { name: "Use demo credentials" }));
    expect(screen.getByLabelText("Email").value).toBe("demo@example.com");

    submitForm();
    await screen.findByRole("heading", { name: "Welcome, Demo User" }, TIMEOUT);

    expect(window.localStorage.getItem(authConfig.rememberEmailKey)).toBe("demo@example.com");
    expect(JSON.parse(window.localStorage.getItem(authConfig.sessionKey))).toEqual({
      email: "demo@example.com",
      displayName: "Demo User"
    });
  });

  it("signs out again, dropping the session but keeping the email", async () => {
    window.localStorage.setItem(
      authConfig.sessionKey,
      JSON.stringify({ email: "demo@example.com", displayName: "Demo User" })
    );
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
    expect(screen.getByRole("heading", { name: "Sign in" })).toBeDefined();
    expect(window.localStorage.getItem(authConfig.sessionKey)).toBeNull();
  });

  it("toggles password visibility and clears the form on Escape", () => {
    render(<App />);
    const password = screen.getByLabelText("Password");
    expect(password.type).toBe("password");

    fireEvent.click(screen.getByRole("button", { name: "Show" }));
    expect(screen.getByLabelText("Password").type).toBe("text");
    fireEvent.click(screen.getByRole("button", { name: "Hide" }));
    expect(screen.getByLabelText("Password").type).toBe("password");

    typeCredentials({ email: "demo@example.com", password: "calculator123" });
    fireEvent.keyDown(screen.getByLabelText("Password"), { key: "Escape" });
    expect(screen.getByLabelText("Email").value).toBe("");
    expect(screen.getByLabelText("Password").value).toBe("");
  });

  it("switches between the dark and light theme", () => {
    render(<App />);
    expect(document.documentElement.dataset.theme).toBe("dark");

    fireEvent.click(screen.getByRole("button", { name: "Light theme" }));
    expect(document.documentElement.dataset.theme).toBe("light");
    fireEvent.click(screen.getByRole("button", { name: "Dark theme" }));
    expect(document.documentElement.dataset.theme).toBe("dark");
  });
});
