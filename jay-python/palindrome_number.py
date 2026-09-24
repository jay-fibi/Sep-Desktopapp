"""Program 6: Palindrome Number Checker.

Checks whether an integer reads the same forwards and backwards, without
converting it to a string. Negative numbers are never palindromes because of
the leading minus sign. Default is 12321.

    python palindrome_number.py 12321
"""

import sys


def is_palindrome_number(number: int) -> bool:
    """Return True if number reads the same forwards and backwards.

    A single digit is always a palindrome and the digit 0 reverses to itself.
    """
    if number < 0:
        return False
    original = number
    reversed_number = 0
    while number > 0:
        number, digit = divmod(number, 10)
        reversed_number = reversed_number * 10 + digit
    return original == reversed_number


def main() -> None:
    number = 12321
    if len(sys.argv) > 1:
        try:
            number = int(sys.argv[1])
        except ValueError:
            print(f"Invalid input '{sys.argv[1]}', using default of 12321.")
    if is_palindrome_number(number):
        print(f"{number} is a palindrome number.")
    else:
        print(f"{number} is not a palindrome number.")


if __name__ == "__main__":
    main()
