"""Program 2: Factorial Calculator.

Computes the factorial of a number. Default is 5.

    python factorial.py 6
"""

import sys


def factorial(n: int) -> int:
    """Return n! for a non-negative integer n."""
    if n < 0:
        raise ValueError("Factorial is not defined for negative numbers.")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def main() -> None:
    number = 5
    if len(sys.argv) > 1:
        try:
            number = int(sys.argv[1])
        except ValueError:
            print(f"Invalid input '{sys.argv[1]}', using default of 5.")
    try:
        print(f"{number}! = {factorial(number)}")
    except ValueError as exc:
        print(f"Error: {exc}")


if __name__ == "__main__":
    main()
