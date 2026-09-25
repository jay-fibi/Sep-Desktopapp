"""Program: Simple Calculator.

Performs basic arithmetic operations (+, -, *, /) on two numbers.
Supports both interactive console mode and command-line arguments.

Usage:
    Interactive mode:
        python calculator.py
    CLI mode:
        python calculator.py 10 + 5
        python calculator.py 10 5
"""

import sys


def add(a: float, b: float) -> float:
    """Return the sum of a and b."""
    return a + b


def subtract(a: float, b: float) -> float:
    """Return the difference of a and b."""
    return a - b


def multiply(a: float, b: float) -> float:
    """Return the product of a and b."""
    return a * b


def divide(a: float, b: float) -> float:
    """Return the division of a by b. Raises ValueError on division by zero."""
    if b == 0:
        raise ValueError("Cannot divide by zero.")
    return a / b


def calculate(a: float, op: str, b: float) -> float:
    """Perform operation op on operands a and b."""
    operations = {
        "+": add,
        "-": subtract,
        "*": multiply,
        "/": divide,
    }
    if op not in operations:
        raise ValueError(f"Unsupported operator '{op}'. Supported operators: +, -, *, /")
    return operations[op](a, b)


def interactive_mode() -> None:
    """Run an interactive prompt loop for user calculations."""
    print("=== Simple Python Calculator ===")
    print("Available operations: + (add), - (subtract), * (multiply), / (divide)")
    print("Enter 'q' or 'quit' to exit.\n")

    while True:
        try:
            choice = input("Enter operator (+, -, *, /) or 'q' to quit: ").strip()
            if choice.lower() in ("q", "quit", "exit"):
                print("Goodbye!")
                break
            if choice not in ("+", "-", "*", "/"):
                print(f"Invalid operator '{choice}'. Please use +, -, *, or /.\n")
                continue

            num1_str = input("Enter first number: ").strip()
            num1 = float(num1_str)

            num2_str = input("Enter second number: ").strip()
            num2 = float(num2_str)

            result = calculate(num1, choice, num2)
            # Format nicely: show integers without decimal point if whole number
            res_str = f"{result:g}"
            print(f"Result: {num1:g} {choice} {num2:g} = {res_str}\n")
        except ValueError as exc:
            print(f"Error: {exc}\n")
        except (KeyboardInterrupt, EOFError):
            print("\nGoodbye!")
            break


def main() -> None:
    args = sys.argv[1:]
    if len(args) == 3:
        # e.g.: python calculator.py 10 + 5
        try:
            num1 = float(args[0])
            op = args[1]
            num2 = float(args[2])
            result = calculate(num1, op, num2)
            print(f"{num1:g} {op} {num2:g} = {result:g}")
        except ValueError as exc:
            print(f"Error: {exc}")
    elif len(args) == 2:
        # e.g.: python calculator.py 10 5 (runs all basic operations)
        try:
            a, b = float(args[0]), float(args[1])
            print(f"{a:g} + {b:g} = {add(a, b):g}")
            print(f"{a:g} - {b:g} = {subtract(a, b):g}")
            print(f"{a:g} * {b:g} = {multiply(a, b):g}")
            try:
                print(f"{a:g} / {b:g} = {divide(a, b):g}")
            except ValueError as exc:
                print(f"{a:g} / {b:g} = Error: {exc}")
        except ValueError as exc:
            print(f"Error: {exc}")
    else:
        interactive_mode()


if __name__ == "__main__":
    main()
