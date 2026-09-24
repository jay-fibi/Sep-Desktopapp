"""Unit tests for palindrome_number.is_palindrome_number."""

import unittest

from palindrome_number import is_palindrome_number


class PalindromeNumberTests(unittest.TestCase):
    def test_palindromes(self):
        for number in (0, 7, 11, 121, 1221, 12321, 1_000_001):
            with self.subTest(number=number):
                self.assertTrue(is_palindrome_number(number))

    def test_non_palindromes(self):
        for number in (-121, 10, 123, 1234, 12_345):
            with self.subTest(number=number):
                self.assertFalse(is_palindrome_number(number))

    def test_boundaries(self):
        self.assertTrue(is_palindrome_number(0))
        self.assertFalse(is_palindrome_number(-1))

    def test_trailing_zero(self):
        self.assertFalse(is_palindrome_number(1000))


if __name__ == "__main__":
    unittest.main()
