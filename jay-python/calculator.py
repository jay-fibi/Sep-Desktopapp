"""Program 2: Simple Calculator.

Performs the four basic arithmetic operations on two numbers.
Run it with no arguments for a demo, or pass numbers via CLI:

    python calculator.py 12 5
"""

import sys


def add(a: float, b: float) -> float:
    return a + b


def subtract(a: float, b: float) -> float:
    return a - b


def multiply(a: float, b: float) -> float:
    return a * b


def divide(a: float, b: float) -> float:
    if b == 0:
        raise ValueError("Cannot divide by zero.")
    return a / b


def demo() -> None:
    x, y = 12, 5
    print(f"{x} + {y} = {add(x, y)}")
    print(f"{x} - {y} = {subtract(x, y)}")
    print(f"{x} * {y} = {multiply(x, y)}")
    print(f"{x} / {y} = {divide(x, y):.2f}")


def main() -> None:
    args = sys.argv[1:]
    if len(args) == 2:
        a, b = float(args[0]), float(args[1])
        print(f"{a} + {b} = {add(a, b)}")
        print(f"{a} - {b} = {subtract(a, b)}")
        print(f"{a} * {b} = {multiply(a, b)}")
        try:
            print(f"{a} / {b} = {divide(a, b):.2f}")
        except ValueError as exc:
            print(f"{a} / {b} = Error: {exc}")
    else:
        demo()


if __name__ == "__main__":
    main()
