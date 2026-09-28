"""Quicksort implementation in Python.

Provides both a simple (functional) version and an in-place version,
plus a small demo when run as a script.
"""

import random


def quicksort(items):
    """Return a new sorted list using the quicksort algorithm.

    Simple, readable version that uses extra memory for partitions.
    Works with any comparable elements.
    """
    items = list(items)
    if len(items) <= 1:
        return items

    pivot = items[len(items) // 2]
    less = [x for x in items if x < pivot]
    equal = [x for x in items if x == pivot]
    greater = [x for x in items if x > pivot]
    return quicksort(less) + equal + quicksort(greater)


def quicksort_in_place(items):
    """Sort a list in place using the Lomuto partition scheme."""
    def partition(lo, hi):
        pivot = items[hi]
        i = lo
        for j in range(lo, hi):
            if items[j] <= pivot:
                items[i], items[j] = items[j], items[i]
                i += 1
        items[i], items[hi] = items[hi], items[i]
        return i

    def sort(lo, hi):
        if lo < hi:
            p = partition(lo, hi)
            sort(lo, p - 1)
            sort(p + 1, hi)

    sort(0, len(items) - 1)
    return items


if __name__ == "__main__":
    data = [random.randint(1, 100) for _ in range(15)]
    print("Original:        ", data)
    print("Sorted (new list):", quicksort(data))
    print("Sorted (in-place):", quicksort_in_place(data.copy()))

    # Sanity checks
    assert quicksort([]) == []
    assert quicksort([1]) == [1]
    assert quicksort([3, 1, 2]) == [1, 2, 3]
    assert quicksort([5, 5, 2, 2, 9]) == [2, 2, 5, 5, 9]
    assert quicksort("cab") == ["a", "b", "c"]
    print("All sanity checks passed.")
