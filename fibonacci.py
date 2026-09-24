"""Fibonacci series program in Python.

Provides functions to generate the Fibonacci sequence iteratively, recursively,
or via a generator, with support for both CLI argument and interactive usage.
"""

import sys
from typing import Generator, List


def fibonacci(n: int) -> List[int]:
    """Return a list containing the first n Fibonacci numbers.

    The sequence starts with 0, 1:
      fibonacci(0) -> []
      fibonacci(1) -> [0]
      fibonacci(2) -> [0, 1]
      fibonacci(5) -> [0, 1, 1, 2, 3]

    Raises:
        ValueError: If n is negative.
    """
    if n < 0:
        raise ValueError("Count of Fibonacci numbers cannot be negative.")
    if n == 0:
        return []

    sequence: List[int] = []
    a, b = 0, 1
    for _ in range(n):
        sequence.append(a)
        a, b = b, a + b
    return sequence


def fibonacci_nth(n: int) -> int:
    """Return the nth Fibonacci number (0-indexed, where F(0) = 0, F(1) = 1).

    Raises:
        ValueError: If n is negative.
    """
    if n < 0:
        raise ValueError("Index cannot be negative.")
    if n == 0:
        return 0
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b


def fibonacci_generator(n: int) -> Generator[int, None, None]:
    """Yield the first n Fibonacci numbers one by one."""
    if n < 0:
        raise ValueError("Count cannot be negative.")
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b


def interactive_mode() -> None:
    """Prompt the user interactively to generate Fibonacci numbers."""
    print("=== Fibonacci Series Generator ===")
    print("Enter the number of terms (or 'quit' / 'exit' to stop).\n")

    while True:
        try:
            line = input("fib> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nGoodbye!")
            break

        if not line:
            continue
        if line.lower() in ("exit", "quit", "q"):
            print("Goodbye!")
            break

        try:
            n = int(line)
            if n < 0:
                print("Error: Please enter a non-negative integer.")
                continue
            series = fibonacci(n)
            print(f"Fibonacci series ({n} terms): {series}")
        except ValueError:
            print(f"Error: '{line}' is not a valid integer.")


def main(argv: list[str] | None = None) -> None:
    """Entry point supporting command-line arguments and interactive mode."""
    if argv is None:
        argv = sys.argv[1:]

    if len(argv) == 1:
        arg = argv[0]
        if arg in ("-h", "--help"):
            print("Usage:")
            print("  python3 fibonacci.py           (interactive prompt or default demo)")
            print("  python3 fibonacci.py <count>   (generate first <count> numbers)")
            return

        try:
            count = int(arg)
            if count < 0:
                print("Error: Count must be a non-negative integer.", file=sys.stderr)
                sys.exit(1)
        except ValueError:
            print(f"Error: Invalid input '{arg}', expected an integer.", file=sys.stderr)
            sys.exit(1)

        print(f"First {count} Fibonacci numbers: {fibonacci(count)}")
    elif len(argv) > 1:
        print("Usage:")
        print("  python3 fibonacci.py           (interactive prompt or default demo)")
        print("  python3 fibonacci.py <count>   (generate first <count> numbers)")
        sys.exit(1)
    else:
        if sys.stdin.isatty():
            interactive_mode()
        else:
            default_count = 10
            print(f"First {default_count} Fibonacci numbers: {fibonacci(default_count)}")


if __name__ == "__main__":
    main()
