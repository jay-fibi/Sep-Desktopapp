/* Shared helpers for the Simple Calculator front-end.
 *
 * Mirrors the number handling of calculator.py (parse_number / format_result).
 * The helpers are published on window.CalcUtils so the page keeps working when
 * index.html is opened straight from disk (no server, no ES modules).
 */
(function (global) {
  "use strict";

  var OPERATORS = ["+", "-", "*", "/"];

  function isOperator(operator) {
    return OPERATORS.indexOf(operator) !== -1;
  }

  function unknownOperatorMessage(operator) {
    return (
      "unknown operator '" + operator +
      "' (use one of: " + OPERATORS.join(" ") + ")"
    );
  }

  /* Convert text to a number. Raises an Error when text is not a number. */
  function parseNumber(text) {
    var raw = String(text).trim();
    if (!raw) {
      throw new Error("'" + text + "' is not a valid number");
    }
    var value = Number(raw);
    if (isNaN(value)) {
      throw new Error("'" + text + "' is not a valid number");
    }
    return value;
  }

  /* Render a result without a trailing '.0' for whole numbers, rounding the
   * rest to `precision` significant digits. */
  function formatNumber(value, precision) {
    if (!isFinite(value)) {
      return String(value);
    }
    if (Number.isInteger(value) && Math.abs(value) < 1e16) {
      return String(value);
    }
    var digits = typeof precision === "number" && precision > 0 ? precision : 12;
    return String(Number(value.toPrecision(digits)));
  }

  var api = {
    OPERATORS: OPERATORS,
    isOperator: isOperator,
    unknownOperatorMessage: unknownOperatorMessage,
    parseNumber: parseNumber,
    formatNumber: formatNumber
  };

  global.CalcUtils = api;
  if (typeof module === "object" && module.exports) {
    module.exports = api; // allows the helpers to be unit-tested with node
  }
})(typeof window !== "undefined" ? window : globalThis);
