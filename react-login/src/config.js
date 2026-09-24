/* Settings for the login screen.
 *
 * Mirrors the `auth` block of ../config.json so the React screen behaves like
 * the vanilla page in login.html. The credentials below are a demo account:
 * they live in the bundle and are only ever compared in the browser.
 */

export const appConfig = {
  name: "Simple Calculator",
  version: "1.0.0"
};

export const authConfig = {
  demoUser: {
    email: "demo@example.com",
    password: "calculator123",
    displayName: "Demo User"
  },
  minPasswordLength: 8,
  maxAttempts: 5,
  sessionKey: "calc.login.session",
  rememberEmailKey: "calc.login.email",
  themeKey: "calc.login.theme"
};
