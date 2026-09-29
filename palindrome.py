"""Palindrome checker.

Case, spaces and punctuation are ignored, so "A man, a plan, a canal:
Panama" counts as a palindrome.

Usage
-----
With arguments (joined into one string)::

    python palindrome.py A man, a plan, a canal: Panama

Without arguments a built-in example is checked.
"""

import sys


def is_palindrome(text):
    """Return True when text reads the same forwards and backwards.

    Only letters and digits are considered, and they are compared
    case-insensitively.
    """
    cleaned = [ch.lower() for ch in text if ch.isalnum()]
    return cleaned == cleaned[::-1]


def check(text):
    """Print whether text is a palindrome and return the exit code."""
    if is_palindrome(text):
        print('"{}" is a palindrome.'.format(text))
        return 0
    print('"{}" is not a palindrome.'.format(text))
    return 1


def main(argv=None):
    """Entry point: check command-line text, or a default example."""
    args = sys.argv[1:] if argv is None else argv
    text = " ".join(args) if args else "A man, a plan, a canal: Panama"
    return check(text)


if __name__ == "__main__":
    sys.exit(main())
