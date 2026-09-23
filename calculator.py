"""A simple command-line calculator.

Usage
-----
Interactive mode (type "quit" or "exit" to leave)::

    python calculator.py

One-shot mode::

    python calculator.py 12 + 5

Supported operators: +  -  *  /  **
"""

import sys

_HELP = """Simple calculator

  Operators : +  -  *  /  **
  Input     : <number> <operator> <number>   e.g. 12 + 5
  Quit      : quit / exit / q
  Help      : help
"""


def add(left, right):
    """Return the sum of left and right."""
    return left + right


def subtract(left, right):
    """Return left minus right."""
    return left - right


def multiply(left, right):
    """Return the product of left and right."""
    return left * right


def divide(left, right):
    """Return left divided by right."""
    if right == 0:
        raise ZeroDivisionError("cannot divide by zero")
    return left / right


def power(left, right):
    """Return left raised to the power of right.

    Raises ValueError when the result is not a real number, which happens
    for a negative base combined with a fractional exponent.
    """
    result = left ** right
    if isinstance(result, complex):
        raise ValueError("result is not a real number")
    return result


OPERATIONS = {
    "+": add,
    "-": subtract,
    "*": multiply,
    "**": power,
    "/": divide,
}


def parse_number(text):
    """Convert text to a float.

    Raises ValueError when text is not a valid number.
    """
    try:
        return float(text)
    except ValueError:
        raise ValueError("'{}' is not a valid number".format(text))


def calculate(left, operator, right):
    """Apply operator to left and right and return the result.

    Raises ValueError for an unknown operator and ZeroDivisionError when
    dividing by zero.
    """
    if operator not in OPERATIONS:
        raise ValueError(
            "unknown operator '{}' (use one of: {})".format(
                operator, " ".join(sorted(OPERATIONS))
            )
        )
    return OPERATIONS[operator](left, right)


def parse_expression(text):
    """Turn "<number> <operator> <number>" into (left, operator, right).

    Raises ValueError when the expression is not well formed.
    """
    parts = text.split()
    if len(parts) != 3:
        raise ValueError("expected '<number> <operator> <number>'")
    left = parse_number(parts[0])
    operator = parts[1]
    right = parse_number(parts[2])
    if operator not in OPERATIONS:
        raise ValueError("unknown operator '{}'".format(operator))
    return left, operator, right


def format_result(value):
    """Render a float result without a trailing '.0' for whole numbers.

    Non-finite values (inf/nan) and very large numbers are left in their
    default textual form.
    """
    if isinstance(value, float) and value.is_integer() and abs(value) < 1e16:
        return str(int(value))
    return str(value)


def evaluate(text):
    """Evaluate a single expression string and return the formatted result.

    Raises ValueError or ZeroDivisionError on bad input.
    """
    left, operator, right = parse_expression(text)
    return format_result(calculate(left, operator, right))


def run_once(expression):
    """Run a single expression from the command line."""
    try:
        print(evaluate(expression))
    except (ValueError, ZeroDivisionError) as error:
        print("Error: {}".format(error), file=sys.stderr)
        return 1
    return 0


def run_interactive():
    """Read expressions from stdin until the user quits."""
    print(_HELP)
    while True:
        try:
            line = input("calc> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if not line:
            continue
        if line.lower() in {"quit", "exit", "q"}:
            break
        if line.lower() in {"help", "h", "?"}:
            print(_HELP)
            continue

        try:
            print(evaluate(line))
        except (ValueError, ZeroDivisionError) as error:
            print("Error: {}".format(error))
    return 0


def main(argv=None):
    """Entry point: one-shot mode with arguments, otherwise interactive."""
    args = sys.argv[1:] if argv is None else argv
    if args:
        return run_once(" ".join(args))
    return run_interactive()


if __name__ == "__main__":
    sys.exit(main())
