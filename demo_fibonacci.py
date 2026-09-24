"""Demo: Fibonacci sequence generator."""


def fibonacci(n):
    """Return the first n Fibonacci numbers."""
    seq = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


if __name__ == "__main__":
    print("First 10 Fibonacci numbers:", fibonacci(10))
