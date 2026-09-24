"""Simple calculator program supporting basic operations, CLI expression evaluation, and interactive mode."""

import sys
from typing import Union

Number = Union[int, float]


def add(a: Number, b: Number) -> Number:
    """Return the sum of a and b."""
    return a + b


def subtract(a: Number, b: Number) -> Number:
    """Return the difference of a and b."""
    return a - b


def multiply(a: Number, b: Number) -> Number:
    """Return the product of a and b."""
    return a * b


def divide(a: Number, b: Number) -> float:
    """Return the quotient of a and b."""
    if b == 0:
        raise ValueError("Cannot divide by zero")
    return a / b


def power(a: Number, b: Number) -> Number:
    """Return a raised to the power of b."""
    return a ** b


def modulo(a: Number, b: Number) -> Number:
    """Return a modulo b."""
    if b == 0:
        raise ValueError("Cannot divide by zero")
    return a % b


def calculate(operation: str, a: Number, b: Number) -> Number:
    """Execute the given arithmetic operation on a and b."""
    ops = {
        "+": add,
        "add": add,
        "-": subtract,
        "sub": subtract,
        "*": multiply,
        "mul": multiply,
        "/": divide,
        "div": divide,
        "^": power,
        "pow": power,
        "%": modulo,
        "mod": modulo,
    }
    op = operation.strip().lower()
    if op not in ops:
        raise ValueError(f"Unknown operation: {operation}")
    return ops[op](a, b)


def interactive_mode() -> None:
    """Run an interactive calculator loop in the terminal."""
    print("=== Simple Python Calculator ===")
    print("Operations: + (add), - (sub), * (mul), / (div), % (mod), ^ (pow)")
    print("Enter 'exit' or 'quit' to stop.\n")

    while True:
        try:
            line = input("calc> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nGoodbye!")
            break

        if not line:
            continue
        if line.lower() in ("exit", "quit", "q"):
            print("Goodbye!")
            break

        tokens = line.split()
        if len(tokens) == 3:
            raw_a, op, raw_b = tokens
            try:
                a = float(raw_a) if "." in raw_a else int(raw_a)
                b = float(raw_b) if "." in raw_b else int(raw_b)
                result = calculate(op, a, b)
                print(f"= {result}")
            except ValueError as err:
                print(f"Error: {err}")
        else:
            print("Usage: <number> <operator> <number> (e.g. 10 + 5)")


def main() -> None:
    if len(sys.argv) == 4:
        # CLI usage: python3 calc.py <num1> <op> <num2>
        raw_a, op, raw_b = sys.argv[1], sys.argv[2], sys.argv[3]
        try:
            a = float(raw_a) if "." in raw_a else int(raw_a)
            b = float(raw_b) if "." in raw_b else int(raw_b)
            result = calculate(op, a, b)
            print(f"{a} {op} {b} = {result}")
        except ValueError as err:
            print(f"Error: {err}")
            sys.exit(1)
    elif len(sys.argv) == 1:
        # Run standard demo if not interactive, else interactive mode
        if sys.stdin.isatty():
            interactive_mode()
        else:
            print("10 + 5 =", add(10, 5))
            print("10 - 5 =", subtract(10, 5))
            print("10 * 5 =", multiply(10, 5))
            print("10 / 5 =", divide(10, 5))
    else:
        print("Usage:")
        print("  python3 calc.py                   (interactive or demo mode)")
        print("  python3 calc.py <num1> <op> <num2> (expression evaluation)")
        sys.exit(1)


if __name__ == "__main__":
    main()
