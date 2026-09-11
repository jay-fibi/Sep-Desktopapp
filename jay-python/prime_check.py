"""Program 4: Prime Number Checker.

Checks whether a number is prime. Default is 29.

    python prime_check.py 97
"""

import sys


def is_prime(n: int) -> bool:
    """Return True if n is a prime number, otherwise False."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0:
        return False
    divisor = 3
    while divisor * divisor <= n:
        if n % divisor == 0:
            return False
        divisor += 2
    return True


def main() -> None:
    number = 29
    if len(sys.argv) > 1:
        try:
            number = int(sys.argv[1])
        except ValueError:
            print(f"Invalid input '{sys.argv[1]}', using default of 29.")
    if is_prime(number):
        print(f"{number} is a prime number.")
    else:
        print(f"{number} is not a prime number.")


if __name__ == "__main__":
    main()
