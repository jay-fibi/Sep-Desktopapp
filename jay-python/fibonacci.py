"""Program 3: Fibonacci Sequence.

Generates the first N Fibonacci numbers. Default is 10.

    python fibonacci.py 8
"""

import sys


def fibonacci(n: int) -> list:
    """Return the first n numbers of the Fibonacci sequence."""
    if n <= 0:
        return []
    sequence = [0]
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
        sequence.append(a)
    return sequence


def main() -> None:
    count = 10
    if len(sys.argv) > 1:
        try:
            count = int(sys.argv[1])
        except ValueError:
            print(f"Invalid input '{sys.argv[1]}', using default of 10.")
    print(f"First {count} Fibonacci numbers: {fibonacci(count)}")


if __name__ == "__main__":
    main()
