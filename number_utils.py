"""Simple number utilities program."""


def is_prime(n: int) -> bool:
    if n < 2:
        return False
    for i in range(2, int(n ** 0.5) + 1):
        if n % i == 0:
            return False
    return True


def fibonacci(count: int) -> list[int]:
    seq = []
    a, b = 0, 1
    for _ in range(count):
        seq.append(a)
        a, b = b, a + b
    return seq


def main() -> None:
    print("Primes up to 30:", [n for n in range(31) if is_prime(n)])
    print("First 10 Fibonacci numbers:", fibonacci(10))


if __name__ == "__main__":
    main()
