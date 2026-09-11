"""Program 1: Hello World.

Prints a friendly greeting to the console.
"""


def greet(name: str = "World") -> str:
    """Return a greeting string for the given name."""
    return f"Hello, {name}!"


def main() -> None:
    print(greet())


if __name__ == "__main__":
    main()
