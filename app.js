/* Simple Calculator - browser front-end.
 *
 * Mirrors calculator.py: an expression is "<number> <operator> <number>" and
 * the supported operators are +  -  *  /.
 * Expects src/utils/format.js (window.CalcUtils) to be loaded first.
 */
(function (global) {
  "use strict";

  var utils = global.CalcUtils;
  if (!utils) {
    console.error("CalcUtils is missing - load src/utils/format.js before app.js");
    return;
  }

  /* Used when config.json cannot be fetched (for example on a file:// page). */
  var DEFAULT_CONFIG = {
    app: { name: "Simple Calculator", version: "1.0.0" },
    display: { precision: 12, theme: "dark", historyLimit: 8 }
  };

  var HELP_TEXT = [
    "Simple calculator",
    "",
    "  Operators : +  -  *  /",
    "  Input     : <number> <operator> <number>   e.g. 12 + 5",
    "  Quit      : Clear button, or the Esc key"
  ].join("\n");

  /* ------------------------------------------------------------- maths --- */

  function add(left, right) {
    return left + right;
  }

  function subtract(left, right) {
    return left - right;
  }

  function multiply(left, right) {
    return left * right;
  }

  function divide(left, right) {
    if (right === 0) {
      throw new Error("cannot divide by zero");
    }
    return left / right;
  }

  var OPERATIONS = {
    "+": add,
    "-": subtract,
    "*": multiply,
    "/": divide
  };

  /* Turn "<number> <operator> <number>" into { left, operator, right }. */
  function parseExpression(text) {
    var parts = String(text).trim().split(/\s+/);
    if (parts.length !== 3) {
      throw new Error("expected '<number> <operator> <number>'");
    }
    var left = utils.parseNumber(parts[0]);
    var operator = parts[1];
    var right = utils.parseNumber(parts[2]);
    if (!utils.isOperator(operator)) {
      throw new Error(utils.unknownOperatorMessage(operator));
    }
    return { left: left, operator: operator, right: right };
  }

  function evaluate(text) {
    var parsed = parseExpression(text);
    return {
      expression: text.trim().replace(/\s+/g, " "),
      value: OPERATIONS[parsed.operator](parsed.left, parsed.right)
    };
  }

  /* ----------------------------------------------------- configuration --- */

  var settings = DEFAULT_CONFIG;

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
          display: Object.assign({}, DEFAULT_CONFIG.display, config.display)
        };
      })
      .catch(function (error) {
        console.warn("config.json unavailable (" + error.message + "), using defaults");
        return DEFAULT_CONFIG;
      });
  }

  /* ------------------------------------------------------------ DOM/UI --- */

  var elements = {};
  var history = [];
  var precision = DEFAULT_CONFIG.display.precision;
  var historyLimit = DEFAULT_CONFIG.display.historyLimit;

  function positiveNumber(value, fallback) {
    var parsed = Number(value);
    return isFinite(parsed) && parsed > 0 ? parsed : fallback;
  }

  function cacheElements() {
    elements.form = document.getElementById("calculator-form");
    elements.expression = document.getElementById("expression");
    elements.result = document.getElementById("result");
    elements.message = document.getElementById("message");
    elements.help = document.getElementById("help");
    elements.history = document.getElementById("history");
    elements.historyCount = document.getElementById("history-count");
    elements.title = document.getElementById("app-title");
    elements.version = document.getElementById("app-version");
  }

  function applySettings() {
    precision = positiveNumber(settings.display.precision, DEFAULT_CONFIG.display.precision);
    historyLimit = positiveNumber(
      settings.display.historyLimit,
      DEFAULT_CONFIG.display.historyLimit
    );
    if (settings.app.name) {
      elements.title.textContent = settings.app.name;
      document.title = settings.app.name;
    }
    if (settings.app.version) {
      elements.version.textContent = "v" + settings.app.version;
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

  function showResult(text) {
    elements.result.textContent = text;
  }

  function addHistoryItem(expression, value) {
    history.unshift({ expression: expression, value: value });
    history = history.slice(0, historyLimit);
    renderHistory();
  }

  function renderHistory() {
    elements.history.textContent = "";
    history.forEach(function (item) {
      var entry = document.createElement("li");
      entry.className = "history-item";
      entry.title = "Click to reuse this expression";

      var expression = document.createElement("span");
      expression.className = "history-expression";
      expression.textContent = item.expression;

      var value = document.createElement("span");
      value.className = "history-value";
      value.textContent = "= " + item.value;

      entry.appendChild(expression);
      entry.appendChild(value);
      entry.addEventListener("click", function () {
        elements.expression.value = item.expression;
        elements.expression.focus();
      });
      elements.history.appendChild(entry);
    });
    elements.historyCount.textContent = String(history.length);
  }

  function clearAll() {
    elements.expression.value = "";
    showResult("0");
    showMessage("");
    elements.expression.focus();
  }

  function appendToken(token) {
    var input = elements.expression;
    if (utils.isOperator(token)) {
      input.value = input.value.replace(/\s+$/, "") + " " + token + " ";
    } else {
      input.value += token;
    }
    input.focus();
  }

  function backspace() {
    var input = elements.expression;
    var tokens = input.value.trim().split(/\s+/).filter(Boolean);
    var last = tokens[tokens.length - 1];

    if (!last) {
      input.value = "";
    } else if (utils.isOperator(last)) {
      tokens.pop(); /* drop the dangling operator */
      input.value = tokens.join(" ");
    } else if (last.length > 1) {
      tokens[tokens.length - 1] = last.slice(0, -1); /* shorten the operand */
      input.value = tokens.join(" ");
    } else {
      tokens.pop(); /* drop a single digit */
      input.value = tokens.join(" ");
      if (utils.isOperator(input.value.slice(-1))) {
        input.value += " "; /* leave room for the next operand */
      }
    }
    input.focus();
  }

  function runCalculation() {
    var text = elements.expression.value.trim();
    var keyword = text.toLowerCase();

    if (!text) {
      showResult("0");
      showMessage("Enter an expression, e.g. 12 + 5", "error");
      return;
    }
    if (["quit", "exit", "q", "clear"].indexOf(keyword) !== -1) {
      clearAll();
      return;
    }
    if (["help", "h", "?"].indexOf(keyword) !== -1) {
      elements.help.hidden = !elements.help.hidden;
      showMessage("Operators: +  -  *  /", "info");
      return;
    }

    try {
      var outcome = evaluate(text);
      var formatted = utils.formatNumber(outcome.value, precision);
      showResult(formatted);
      showMessage("OK", "ok");
      addHistoryItem(outcome.expression, formatted);
    } catch (error) {
      showResult("-");
      showMessage("Error: " + error.message, "error");
    }
  }

  function handleAction(action) {
    switch (action) {
      case "calculate":
        runCalculation();
        break;
      case "clear":
        clearAll();
        break;
      case "back":
        backspace();
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
    elements.form.addEventListener("submit", function (event) {
      event.preventDefault();
      runCalculation();
    });

    document.addEventListener("click", function (event) {
      var target = event.target;
      if (!target || typeof target.closest !== "function") {
        return;
      }
      var trigger = target.closest("[data-action], [data-token]");
      if (!trigger) {
        return;
      }
      var action = trigger.getAttribute("data-action");
      var token = trigger.getAttribute("data-token");
      if (action) {
        event.preventDefault();
        handleAction(action);
      } else if (token !== null) {
        appendToken(token);
      }
    });

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        clearAll();
      }
    });
  }

  function init() {
    cacheElements();
    bindEvents();
    renderHistory();
    loadConfig().then(function (config) {
      settings = config;
      applySettings();
      elements.expression.focus();
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})(window);
