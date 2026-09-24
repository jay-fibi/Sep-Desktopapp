"""Program 5: Palindrome Checker.

Checks whether a word or phrase reads the same forwards and backwards,
ignoring case, spaces and punctuation. Default is "A man, a plan, a canal: Panama".

    python palindrome.py "racecar"
"""

import sys


def is_palindrome(text: str) -> bool:
    """Return True if the cleaned text is a palindrome, otherwise False."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def main() -> None:
    text = "A man, a plan, a canal: Panama"
    if len(sys.argv) > 1:
        text = " ".join(sys.argv[1:])
    if is_palindrome(text):
        print(f'"{text}" is a palindrome.')
    else:
        print(f'"{text}" is not a palindrome.')


if __name__ == "__main__":
    main()
