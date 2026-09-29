"""Merge Sort program."""


def merge(left: list[int], right: list[int]) -> list[int]:
    """Merge two sorted lists into one sorted list."""
    merged: list[int] = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    # Append any remaining elements
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_sort(arr: list[int]) -> list[int]:
    """Sort a list using the merge sort algorithm (O(n log n)).

    Returns a new sorted list and does not modify the input.
    """
    if len(arr) <= 1:
        return arr[:]
    mid = len(arr) // 2
    left_sorted = merge_sort(arr[:mid])
    right_sorted = merge_sort(arr[mid:])
    return merge(left_sorted, right_sorted)


def main() -> None:
    examples = [
        [38, 27, 43, 3, 9, 82, 10],
        [5, 2, 4, 6, 1, 3],
        [],
        [1],
        [5, 5, 5, 1, 1, 3],
        [9, 8, 7, 6, 5, 4, 3, 2, 1],
    ]
    for arr in examples:
        print(f"Original: {arr} -> Sorted: {merge_sort(arr)}")

    # Interactive part: let user enter their own list
    try:
        user_input = input("\nEnter numbers separated by spaces (or press Enter to skip): ").strip()
        if user_input:
            user_list = [int(x) for x in user_input.split()]
            print(f"Original: {user_list} -> Sorted: {merge_sort(user_list)}")
    except ValueError:
        print("Invalid input. Please enter integers separated by spaces.")
    except (EOFError, KeyboardInterrupt):
        print("\nSkipped user input.")


if __name__ == "__main__":
    main()
