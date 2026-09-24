"""Tests for the Fibonacci module."""

import unittest
from fibonacci import fibonacci, fibonacci_nth, fibonacci_generator


class TestFibonacci(unittest.TestCase):
    def test_fibonacci_zero(self):
        self.assertEqual(fibonacci(0), [])

    def test_fibonacci_one(self):
        self.assertEqual(fibonacci(1), [0])

    def test_fibonacci_two(self):
        self.assertEqual(fibonacci(2), [0, 1])

    def test_fibonacci_ten(self):
        expected = [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
        self.assertEqual(fibonacci(10), expected)

    def test_fibonacci_negative(self):
        with self.assertRaises(ValueError):
            fibonacci(-1)

    def test_fibonacci_nth(self):
        self.assertEqual(fibonacci_nth(0), 0)
        self.assertEqual(fibonacci_nth(1), 1)
        self.assertEqual(fibonacci_nth(2), 1)
        self.assertEqual(fibonacci_nth(3), 2)
        self.assertEqual(fibonacci_nth(4), 3)
        self.assertEqual(fibonacci_nth(5), 5)
        self.assertEqual(fibonacci_nth(9), 34)

    def test_fibonacci_nth_negative(self):
        with self.assertRaises(ValueError):
            fibonacci_nth(-1)

    def test_fibonacci_generator(self):
        self.assertEqual(list(fibonacci_generator(0)), [])
        self.assertEqual(list(fibonacci_generator(1)), [0])
        self.assertEqual(list(fibonacci_generator(7)), [0, 1, 1, 2, 3, 5, 8])

    def test_fibonacci_generator_negative(self):
        with self.assertRaises(ValueError):
            list(fibonacci_generator(-1))


if __name__ == "__main__":
    unittest.main()
