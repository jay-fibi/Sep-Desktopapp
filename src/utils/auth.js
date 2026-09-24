/* Shared helpers for the Simple Calculator sign-in page.
 *
 * Mirrors the validation style of calculator.py and src/utils/format.js:
 * small pure functions that return plain-text messages (an empty string means
 * "no problem") instead of touching the DOM.
 * The helpers are published on window.AuthUtils so login.html keeps working
 * when it is opened straight from disk (no server, no ES modules).
 */
(function (global) {
  "use strict";

  /* Deliberately permissive: local part, '@', domain and a 2+ letter TLD. */
  var EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/;
  var DEFAULT_MIN_PASSWORD_LENGTH = 8;

  function asText(value) {
    return value === undefined || value === null ? "" : String(value);
  }

  function normalizeEmail(email) {
    return asText(email).trim().toLowerCase();
  }

  function isValidEmail(email) {
    return EMAIL_PATTERN.test(normalizeEmail(email));
  }

  /* Message for the email field, "" when the value is acceptable. */
  function emailError(email) {
    var value = normalizeEmail(email);
    if (!value) {
      return "Enter your email address";
    }
    if (!isValidEmail(value)) {
      return "'" + asText(email).trim() + "' is not a valid email address";
    }
    return "";
  }

  function minPasswordLength(value) {
    var parsed = Number(value);
    return isFinite(parsed) && parsed > 0
      ? Math.floor(parsed)
      : DEFAULT_MIN_PASSWORD_LENGTH;
  }

  /* Message for the password field, "" when the value is acceptable. */
  function passwordError(password, limit) {
    var value = asText(password);
    var required = minPasswordLength(limit);
    if (!value) {
      return "Enter your password";
    }
    if (value.length < required) {
      return "Password must be at least " + required + " characters";
    }
    return "";
  }

  /* Validate a whole sign-in form and return the normalised email plus one
   * message per field. */
  function validateCredentials(email, password, limit) {
    var errors = {
      email: emailError(email),
      password: passwordError(password, limit)
    };
    return {
      valid: !errors.email && !errors.password,
      email: normalizeEmail(email),
      errors: errors
    };
  }

  /* Compare the typed values with a single demo user ({ email, password }).
   * This is a UI demo, not real authentication. */
  function credentialsMatch(email, password, user) {
    if (!user) {
      return false;
    }
    return (
      normalizeEmail(email) === normalizeEmail(user.email) &&
      asText(password) === asText(user.password)
    );
  }

  /* demo@example.com -> d***o@example.com, for status messages and logs. */
  function maskEmail(email) {
    var value = normalizeEmail(email);
    var at = value.indexOf("@");
    if (at < 1) {
      return value;
    }
    var local = value.slice(0, at);
    var domain = value.slice(at);
    if (local.length === 1) {
      return local + "***" + domain;
    }
    return local.charAt(0) + "***" + local.charAt(local.length - 1) + domain;
  }

  var api = {
    DEFAULT_MIN_PASSWORD_LENGTH: DEFAULT_MIN_PASSWORD_LENGTH,
    normalizeEmail: normalizeEmail,
    isValidEmail: isValidEmail,
    emailError: emailError,
    minPasswordLength: minPasswordLength,
    passwordError: passwordError,
    validateCredentials: validateCredentials,
    credentialsMatch: credentialsMatch,
    maskEmail: maskEmail
  };

  global.AuthUtils = api;
  if (typeof module === "object" && module.exports) {
    module.exports = api; // allows the helpers to be unit-tested with node
  }
})(typeof window !== "undefined" ? window : globalThis);
