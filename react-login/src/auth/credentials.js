/* Validation helpers for the login screen.
 *
 * Same rules as src/utils/auth.js in the vanilla page: every function is pure,
 * returns plain text, and an empty string from emailError/passwordError means
 * "no problem". Keeping them framework-free makes them trivial to unit test.
 */

/* Deliberately permissive: local part, '@', domain and a 2+ letter TLD. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/;

export const DEFAULT_MIN_PASSWORD_LENGTH = 8;

function asText(value) {
  return value === undefined || value === null ? "" : String(value);
}

export function normalizeEmail(email) {
  return asText(email).trim().toLowerCase();
}

export function isValidEmail(email) {
  return EMAIL_PATTERN.test(normalizeEmail(email));
}

/* Message for the email field, "" when the value is acceptable. */
export function emailError(email) {
  const value = normalizeEmail(email);
  if (!value) {
    return "Enter your email address";
  }
  if (!isValidEmail(value)) {
    return `'${asText(email).trim()}' is not a valid email address`;
  }
  return "";
}

export function minPasswordLength(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0
    ? Math.floor(parsed)
    : DEFAULT_MIN_PASSWORD_LENGTH;
}

/* Message for the password field, "" when the value is acceptable. */
export function passwordError(password, limit) {
  const value = asText(password);
  const required = minPasswordLength(limit);
  if (!value) {
    return "Enter your password";
  }
  if (value.length < required) {
    return `Password must be at least ${required} characters`;
  }
  return "";
}

/* Validate a whole sign-in form: the normalised email plus one message per
 * field. */
export function validateCredentials(email, password, limit) {
  const errors = {
    email: emailError(email),
    password: passwordError(password, limit)
  };
  return {
    valid: !errors.email && !errors.password,
    email: normalizeEmail(email),
    errors
  };
}

/* Compare the typed values with a single demo user ({ email, password }).
 * This is a UI demo, not real authentication. */
export function credentialsMatch(email, password, user) {
  if (!user) {
    return false;
  }
  return (
    normalizeEmail(email) === normalizeEmail(user.email) &&
    asText(password) === asText(user.password)
  );
}

/* demo@example.com -> d***o@example.com, for status messages and logs. */
export function maskEmail(email) {
  const value = normalizeEmail(email);
  const at = value.indexOf("@");
  if (at < 1) {
    return value;
  }
  const local = value.slice(0, at);
  const domain = value.slice(at);
  if (local.length === 1) {
    return `${local}***${domain}`;
  }
  return `${local.charAt(0)}***${local.charAt(local.length - 1)}${domain}`;
}
