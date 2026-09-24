/* Simple Calculator - sign-in page front-end.
 *
 * The typed credentials are compared in the browser with the demo user from
 * config.json; nothing is sent anywhere and no real authentication happens.
 * Expects src/utils/auth.js (window.AuthUtils) to be loaded first.
 */
(function (global) {
  "use strict";

  var utils = global.AuthUtils;
  if (!utils) {
    console.error("AuthUtils is missing - load src/utils/auth.js before login.js");
    return;
  }

  /* Used when config.json cannot be fetched (for example on a file:// page). */
  var DEFAULT_CONFIG = {
    app: { name: "Simple Calculator", version: "1.0.0" },
    display: { theme: "dark" },
    auth: {
      demoUser: {
        email: "demo@example.com",
        password: "calculator123",
        displayName: "Demo User"
      },
      minPasswordLength: 8,
      maxAttempts: 5,
      redirectUrl: "index.html",
      rememberEmailKey: "calc.login.email",
      sessionKey: "calc.login.session"
    }
  };

  var HELP_TEXT = [
    "Sign in",
    "",
    "  Demo user : printed under the form (or press 'Use demo credentials')",
    "  Enter     : submit the form",
    "  Esc       : clear the form",
    "  Storage   : the session is kept in localStorage, nothing is uploaded"
  ].join("\n");

  /* --------------------------------------------------------- page state --- */

  var settings = DEFAULT_CONFIG;
  var elements = {};
  var fields = {};
  var attempts = 0;
  var locked = false;
  var minPasswordLength = DEFAULT_CONFIG.auth.minPasswordLength;
  var maxAttempts = DEFAULT_CONFIG.auth.maxAttempts;

  function positiveNumber(value, fallback) {
    var parsed = Number(value);
    return isFinite(parsed) && parsed > 0 ? parsed : fallback;
  }

  /* ------------------------------------------------------ browser store --- */

  function readStore(key) {
    try {
      return global.localStorage ? global.localStorage.getItem(key) : null;
    } catch (error) {
      console.warn("localStorage unavailable (" + error.message + ")");
      return null;
    }
  }

  function writeStore(key, value) {
    try {
      if (global.localStorage) {
        global.localStorage.setItem(key, value);
      }
    } catch (error) {
      console.warn("localStorage unavailable (" + error.message + ")");
    }
  }

  function removeStore(key) {
    try {
      if (global.localStorage) {
        global.localStorage.removeItem(key);
      }
    } catch (error) {
      console.warn("localStorage unavailable (" + error.message + ")");
    }
  }

  /* ------------------------------------------------------ configuration --- */

  function mergeAuth(auth) {
    var merged = Object.assign({}, DEFAULT_CONFIG.auth, auth);
    merged.demoUser = Object.assign(
      {},
      DEFAULT_CONFIG.auth.demoUser,
      auth ? auth.demoUser : null
    );
    return merged;
  }

  function loadConfig() {
    if (typeof global.fetch !== "function") {
      return Promise.resolve(DEFAULT_CONFIG);
    }
    return global
      .fetch("config.json", { cache: "no-store" })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("HTTP " + response.status);
        }
        return response.json();
      })
      .then(function (config) {
        return {
          app: Object.assign({}, DEFAULT_CONFIG.app, config.app),
          display: Object.assign({}, DEFAULT_CONFIG.display, config.display),
          auth: mergeAuth(config.auth)
        };
      })
      .catch(function (error) {
        console.warn("config.json unavailable (" + error.message + "), using defaults");
        return DEFAULT_CONFIG;
      });
  }

  /* ------------------------------------------------------------ DOM/UI --- */

  function cacheElements() {
    elements.form = document.getElementById("login-form");
    elements.email = document.getElementById("email");
    elements.password = document.getElementById("password");
    elements.remember = document.getElementById("remember");
    elements.passwordHint = document.getElementById("password-hint");
    elements.message = document.getElementById("message");
    elements.help = document.getElementById("help");
    elements.credentials = document.getElementById("demo-credentials");
    elements.signIn = document.getElementById("sign-in");
    elements.signOut = document.getElementById("sign-out");
    elements.togglePassword = document.getElementById("toggle-password");

    fields.email = {
      input: elements.email,
      error: document.getElementById("email-error")
    };
    fields.password = {
      input: elements.password,
      error: document.getElementById("password-error")
    };
  }

  function applySettings() {
    minPasswordLength = positiveNumber(
      settings.auth.minPasswordLength,
      DEFAULT_CONFIG.auth.minPasswordLength
    );
    maxAttempts = positiveNumber(settings.auth.maxAttempts, DEFAULT_CONFIG.auth.maxAttempts);
    if (settings.app.name) {
      document.title = "Sign in - " + settings.app.name;
    }
    if (elements.passwordHint) {
      elements.passwordHint.textContent = "At least " + minPasswordLength + " characters.";
    }
    if (elements.credentials) {
      var user = settings.auth.demoUser;
      elements.credentials.textContent = user.email + " / " + user.password;
    }
    elements.help.textContent = HELP_TEXT;
    setTheme(settings.display.theme === "dark" ? "dark" : "light");
  }

  function setTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
  }

  function toggleTheme() {
    var current = document.documentElement.getAttribute("data-theme");
    setTheme(current === "dark" ? "light" : "dark");
  }

  function showMessage(text, kind) {
    elements.message.textContent = text;
    elements.message.className = "message" + (kind ? " message-" + kind : "");
  }

  function setFieldError(field, text) {
    field.error.textContent = text || "";
    if (text) {
      field.input.setAttribute("aria-invalid", "true");
    } else {
      field.input.removeAttribute("aria-invalid");
    }
  }

  function clearFieldErrors() {
    setFieldError(fields.email, "");
    setFieldError(fields.password, "");
  }

  function fieldMessage(name, value) {
    return name === "email"
      ? utils.emailError(value)
      : utils.passwordError(value, minPasswordLength);
  }

  /* Validate one field and return its message ("" when acceptable). */
  function validateField(name) {
    var field = fields[name];
    if (!field.input.value) {
      setFieldError(field, ""); /* stay quiet on an empty field */
      return "";
    }
    var message = fieldMessage(name, field.input.value);
    setFieldError(field, message);
    return message;
  }
  /* ---------------------------------------------------------- session --- */

  function readSession() {
    var raw = readStore(settings.auth.sessionKey);
    if (!raw) {
      return null;
    }
    try {
      var session = JSON.parse(raw);
      return session && session.email ? session : null;
    } catch (error) {
      console.warn("Ignoring unreadable session (" + error.message + ")");
      removeStore(settings.auth.sessionKey);
      return null;
    }
  }

  function createSession(email) {
    var session = { email: email, signedInAt: new Date().toISOString() };
    writeStore(settings.auth.sessionKey, JSON.stringify(session));
    return session;
  }

  function clearSession() {
    removeStore(settings.auth.sessionKey);
  }

  /* ---------------------------------------------------------- actions --- */

  function togglePassword() {
    var hidden = elements.password.type === "password";
    elements.password.type = hidden ? "text" : "password";
    elements.togglePassword.textContent = hidden ? "Hide" : "Show";
    elements.togglePassword.setAttribute("aria-pressed", hidden ? "true" : "false");
    elements.password.focus();
  }

  function registerFailure() {
    attempts += 1;
    elements.password.value = "";
    elements.password.focus();
    if (attempts >= maxAttempts) {
      locked = true;
      elements.signIn.disabled = true;
      showMessage("Too many failed attempts - reload the page to try again", "error");
      return;
    }
    showMessage(
      "Error: wrong email or password (" + attempts + " of " + maxAttempts + " attempts)",
      "error"
    );
  }

  function redirectAfterSignIn() {
    if (settings.auth.redirectUrl) {
      global.location.assign(settings.auth.redirectUrl);
    }
  }

  function signIn() {
    if (locked) {
      showMessage("Too many failed attempts - reload the page to try again", "error");
      return;
    }

    var password = elements.password.value;
    var check = utils.validateCredentials(elements.email.value, password, minPasswordLength);
    setFieldError(fields.email, check.errors.email);
    setFieldError(fields.password, check.errors.password);

    if (!check.valid) {
      showMessage("Please fix the highlighted fields", "error");
      (check.errors.email ? elements.email : elements.password).focus();
      return;
    }

    if (!utils.credentialsMatch(check.email, password, settings.auth.demoUser)) {
      registerFailure();
      return;
    }

    attempts = 0;
    createSession(check.email);
    if (elements.remember.checked) {
      writeStore(settings.auth.rememberEmailKey, check.email);
    } else {
      removeStore(settings.auth.rememberEmailKey);
    }
    elements.signOut.hidden = false;
    showMessage(
      "Signed in as " + check.email +
        (settings.auth.redirectUrl
          ? " - redirecting to " + settings.auth.redirectUrl + " ..."
          : ""),
      "ok"
    );
    global.setTimeout(redirectAfterSignIn, 800);
  }

  function signOut() {
    var session = readSession();
    clearSession();
    elements.signOut.hidden = true;
    elements.password.value = "";
    clearFieldErrors();
    showMessage(
      session ? "Signed out from " + utils.maskEmail(session.email) : "Signed out",
      "info"
    );
    elements.email.focus();
  }

  function fillDemoCredentials() {
    var user = settings.auth.demoUser;
    elements.email.value = user.email;
    elements.password.value = user.password;
    clearFieldErrors();
    showMessage("Demo credentials filled in - press Sign in", "info");
    elements.signIn.focus();
  }

  function forgotPassword() {
    showMessage(
      "Demo page - there is no password reset. Use the account listed under the form.",
      "info"
    );
  }

  function clearForm() {
    elements.form.reset();
    clearFieldErrors();
    showMessage("");
    elements.email.focus();
  }

  function restoreState() {
    var remembered = readStore(settings.auth.rememberEmailKey);
    if (remembered) {
      elements.email.value = remembered;
      elements.remember.checked = true;
    }

    var session = readSession();
    if (session) {
      elements.email.value = session.email;
      elements.signOut.hidden = false;
      showMessage("Already signed in as " + session.email + " - sign in again to continue", "info");
    }
  }
  /* ----------------------------------------------------------- wiring --- */

  function handleAction(action) {
    switch (action) {
      case "toggle-password":
        togglePassword();
        break;
      case "fill-demo":
        fillDemoCredentials();
        break;
      case "forgot-password":
        forgotPassword();
        break;
      case "sign-out":
        signOut();
        break;
      case "toggle-help":
        elements.help.hidden = !elements.help.hidden;
        break;
      case "toggle-theme":
        toggleTheme();
        break;
      default:
        console.warn("Unknown action: " + action);
    }
  }

  function bindEvents() {
    /* The Sign in button is type="submit", so click and Enter both land here
     * exactly once (no data-action, which would swallow the submit). */
    elements.form.addEventListener("submit", function (event) {
      event.preventDefault();
      signIn();
    });

    document.addEventListener("click", function (event) {
      var target = event.target;
      if (!target || typeof target.closest !== "function") {
        return;
      }
      var trigger = target.closest("[data-action]");
      if (!trigger) {
        return;
      }
      event.preventDefault();
      handleAction(trigger.getAttribute("data-action"));
    });

    Object.keys(fields).forEach(function (name) {
      var field = fields[name];
      field.input.addEventListener("blur", function () {
        validateField(name);
      });
      field.input.addEventListener("input", function () {
        if (field.input.getAttribute("aria-invalid") === "true") {
          validateField(name); /* clear the error as soon as it is fixed */
        }
      });
    });

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        clearForm();
      }
    });
  }

  function init() {
    cacheElements();
    bindEvents();
    loadConfig().then(function (config) {
      settings = config;
      applySettings();
      restoreState();
      elements.email.focus();
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})(window);
