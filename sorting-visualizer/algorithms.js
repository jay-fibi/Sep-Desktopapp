/*
 * algorithms.js — shared core for the Java Sorting Visualizer.
 *
 * Contains, for each of six classic sorting algorithms:
 *   1. JAVA_CODE  — the reference Java source shown in the code panel.
 *   2. META       — complexity + description shown in the info card.
 *   3. A generator — mirrors the Java code step by step and yields events
 *      ({ type, i, j, value, lines }) so the UI can animate the bars and
 *      highlight the exact Java line being "executed".
 *
 * Works both in the browser (exposes window.SORTING) and in Node
 * (module.exports) so the logic can be unit-tested headlessly.
 */
(function (root, factory) {
  const api = factory();
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  } else {
    root.SORTING = api;
  }
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  /* ------------------------------------------------------------------ *
   * Java source snippets.                                              *
   * IMPORTANT: the `lines` arrays emitted by the generators below are  *
   * 1-based line numbers into these exact strings — keep them in sync! *
   * ------------------------------------------------------------------ */
  const JAVA_CODE = {
    bubble: `public static void bubbleSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        boolean swapped = false;
        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                // Swap adjacent elements
                int temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
                swapped = true;
            }
        }
        if (!swapped) break; // Already sorted
    }
}`,
    selection: `public static void selectionSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        int minIndex = i;
        for (int j = i + 1; j < n; j++) {
            if (arr[j] < arr[minIndex]) {
                minIndex = j;
            }
        }
        // Swap the found minimum with the first element
        int temp = arr[minIndex];
        arr[minIndex] = arr[i];
        arr[i] = temp;
    }
}`,
    insertion: `public static void insertionSort(int[] arr) {
    for (int i = 1; i < arr.length; i++) {
        int key = arr[i];
        int j = i - 1;
        // Shift elements greater than key one position ahead
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j];
            j--;
        }
        arr[j + 1] = key;
    }
}`,
    merge: `public static void mergeSort(int[] arr, int left, int right) {
    if (left < right) {
        int mid = (left + right) / 2;
        mergeSort(arr, left, mid);       // Sort left half
        mergeSort(arr, mid + 1, right);  // Sort right half
        merge(arr, left, mid, right);    // Merge sorted halves
    }
}

private static void merge(int[] arr, int left, int mid, int right) {
    int[] temp = new int[right - left + 1];
    int i = left, j = mid + 1, k = 0;
    while (i <= mid && j <= right) {
        temp[k++] = (arr[i] <= arr[j]) ? arr[i++] : arr[j++];
    }
    while (i <= mid) temp[k++] = arr[i++];
    while (j <= right) temp[k++] = arr[j++];
    for (i = left, k = 0; i <= right; i++, k++) {
        arr[i] = temp[k];
    }
}`,
    quick: `public static void quickSort(int[] arr, int low, int high) {
    if (low < high) {
        int pivotIndex = partition(arr, low, high);
        quickSort(arr, low, pivotIndex - 1);  // Sort left part
        quickSort(arr, pivotIndex + 1, high); // Sort right part
    }
}

private static int partition(int[] arr, int low, int high) {
    int pivot = arr[high];
    int i = low - 1;
    for (int j = low; j < high; j++) {
        if (arr[j] < pivot) {
            i++;
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }
    int temp = arr[i + 1];
    arr[i + 1] = arr[high];
    arr[high] = temp;
    return i + 1;
}`,
    heap: `public static void heapSort(int[] arr) {
    int n = arr.length;
    // Build max heap
    for (int i = n / 2 - 1; i >= 0; i--) {
        heapify(arr, n, i);
    }
    // Extract elements from the heap one by one
    for (int i = n - 1; i > 0; i--) {
        int temp = arr[0];
        arr[0] = arr[i];
        arr[i] = temp;
        heapify(arr, i, 0);
    }
}

private static void heapify(int[] arr, int n, int i) {
    int largest = i;
    int left = 2 * i + 1;
    int right = 2 * i + 2;
    if (left < n && arr[left] > arr[largest]) largest = left;
    if (right < n && arr[right] > arr[largest]) largest = right;
    if (largest != i) {
        int temp = arr[i];
        arr[i] = arr[largest];
        arr[largest] = temp;
        heapify(arr, n, largest);
    }
}`,
  };

  /* ------------------------------------------------------------------ *
   * Metadata: complexities, stability, description.                    *
   * ------------------------------------------------------------------ */
  const META = {
    bubble: {
      name: 'Bubble Sort',
      best: 'O(n)', average: 'O(n²)', worst: 'O(n²)', space: 'O(1)', stable: 'Yes',
      description:
        'Repeatedly steps through the array, comparing each pair of adjacent elements and swapping them if they are out of order. After every pass, the largest unsorted element "bubbles up" to its final position. Simple, but slow on large data.',
    },
    selection: {
      name: 'Selection Sort',
      best: 'O(n²)', average: 'O(n²)', worst: 'O(n²)', space: 'O(1)', stable: 'No',
      description:
        'Scans the unsorted region for the smallest element and swaps it into the next slot of the sorted region. Makes the fewest swaps of any algorithm here (at most n−1), but always performs O(n²) comparisons.',
    },
    insertion: {
      name: 'Insertion Sort',
      best: 'O(n)', average: 'O(n²)', worst: 'O(n²)', space: 'O(1)', stable: 'Yes',
      description:
        'Builds the sorted array one element at a time: it takes the next element and shifts larger elements one slot right until the key can drop into place. Excellent on small or nearly-sorted arrays — Java itself uses it (inside TimSort) for tiny runs.',
    },
    merge: {
      name: 'Merge Sort',
      best: 'O(n log n)', average: 'O(n log n)', worst: 'O(n log n)', space: 'O(n)', stable: 'Yes',
      description:
        'Divide and conquer: recursively split the array into halves until they are trivially sorted, then merge the sorted halves back together. Guaranteed O(n log n), at the cost of a temporary array.',
    },
    quick: {
      name: 'Quick Sort',
      best: 'O(n log n)', average: 'O(n log n)', worst: 'O(n²)', space: 'O(log n)', stable: 'No',
      description:
        'Picks a pivot (here: the last element) and partitions the array so everything smaller lands to its left and everything larger to its right, then recurses on both sides. The fastest general-purpose sort in practice — Java uses a dual-pivot variant for primitives.',
    },
    heap: {
      name: 'Heap Sort',
      best: 'O(n log n)', average: 'O(n log n)', worst: 'O(n log n)', space: 'O(1)', stable: 'No',
      description:
        'Turns the array into a max heap, then repeatedly swaps the root (the maximum) with the last unsorted element and sifts the new root back down. O(n log n) guaranteed with O(1) extra space.',
    },
  };

  /* ------------------------------------------------------------------ *
   * Step generators. Each mutates `a` in place and yields events:      *
   *   { type: 'compare',    i, j, lines }   — two bars being compared  *
   *   { type: 'swap',       i, j, lines }   — two bars swapped         *
   *   { type: 'set',        i, value, lines } — single bar overwritten *
   *   { type: 'pivot',      i, lines }      — pivot / key element      *
   *   { type: 'markSorted', i, lines }      — index reached final spot *
   *   { type: 'note',       lines }         — pure code highlight      *
   * `lines` are 1-based numbers into JAVA_CODE[algo].                  *
   * ------------------------------------------------------------------ */

  function* bubbleSort(a) {
    const n = a.length;
    for (let i = 0; i < n - 1; i++) {
      let swapped = false;
      for (let j = 0; j < n - i - 1; j++) {
        yield { type: 'compare', i: j, j: j + 1, lines: [6] };
        if (a[j] > a[j + 1]) {
          const temp = a[j]; a[j] = a[j + 1]; a[j + 1] = temp;
          swapped = true;
          yield { type: 'swap', i: j, j: j + 1, lines: [8, 9, 10] };
        }
      }
      yield { type: 'markSorted', i: n - i - 1, lines: [13] };
      if (!swapped) {
        yield { type: 'note', lines: [14] };
        break;
      }
    }
  }

  function* selectionSort(a) {
    const n = a.length;
    for (let i = 0; i < n - 1; i++) {
      let minIndex = i;
      yield { type: 'pivot', i: minIndex, lines: [4] };
      for (let j = i + 1; j < n; j++) {
        yield { type: 'compare', i: j, j: minIndex, lines: [6] };
        if (a[j] < a[minIndex]) {
          minIndex = j;
          yield { type: 'pivot', i: minIndex, lines: [7] };
        }
      }
      if (minIndex !== i) {
        const temp = a[minIndex]; a[minIndex] = a[i]; a[i] = temp;
        yield { type: 'swap', i: minIndex, j: i, lines: [11, 12, 13] };
      }
      yield { type: 'markSorted', i, lines: [13] };
    }
  }

  function* insertionSort(a) {
    for (let i = 1; i < a.length; i++) {
      const key = a[i];
      yield { type: 'pivot', i, lines: [3] };
      let j = i - 1;
      while (j >= 0) {
        yield { type: 'compare', i: j, j: j + 1, lines: [6] };
        if (a[j] <= key) break;
        a[j + 1] = a[j];
        yield { type: 'set', i: j + 1, value: a[j], lines: [7] };
        j--;
      }
      a[j + 1] = key;
      yield { type: 'set', i: j + 1, value: key, lines: [10] };
    }
  }

  function* mergeSort(a, left = 0, right = a.length - 1) {
    if (left < right) {
      const mid = (left + right) >> 1;
      yield { type: 'note', lines: [3] };
      yield* mergeSort(a, left, mid);
      yield* mergeSort(a, mid + 1, right);
      yield* merge(a, left, mid, right);
    }
  }

  function* merge(a, left, mid, right) {
    const temp = new Array(right - left + 1);
    let i = left, j = mid + 1, k = 0;
    while (i <= mid && j <= right) {
      yield { type: 'compare', i, j, lines: [13, 14] };
      if (a[i] <= a[j]) temp[k++] = a[i++];
      else temp[k++] = a[j++];
    }
    while (i <= mid) { temp[k++] = a[i++]; yield { type: 'note', lines: [16] }; }
    while (j <= right) { temp[k++] = a[j++]; yield { type: 'note', lines: [17] }; }
    for (i = left, k = 0; i <= right; i++, k++) {
      a[i] = temp[k];
      yield { type: 'set', i, value: temp[k], lines: [18, 19] };
    }
  }

  function* quickSort(a, low = 0, high = a.length - 1) {
    if (low < high) {
      const pivotIndex = yield* partition(a, low, high);
      yield { type: 'markSorted', i: pivotIndex, lines: [3] };
      yield* quickSort(a, low, pivotIndex - 1);
      yield* quickSort(a, pivotIndex + 1, high);
    } else if (low === high) {
      yield { type: 'markSorted', i: low, lines: [2] };
    }
  }

  function* partition(a, low, high) {
    const pivot = a[high];
    yield { type: 'pivot', i: high, lines: [10] };
    let i = low - 1;
    for (let j = low; j < high; j++) {
      yield { type: 'compare', i: j, j: high, lines: [13] };
      if (a[j] < pivot) {
        i++;
        const temp = a[i]; a[i] = a[j]; a[j] = temp;
        yield { type: 'swap', i, j, lines: [15, 16, 17] };
      }
    }
    const temp = a[i + 1]; a[i + 1] = a[high]; a[high] = temp;
    yield { type: 'swap', i: i + 1, j: high, lines: [20, 21, 22] };
    return i + 1;
  }

  function* heapSort(a) {
    const n = a.length;
    for (let i = (n >> 1) - 1; i >= 0; i--) {
      yield { type: 'note', lines: [4, 5] };
      yield* heapify(a, n, i);
    }
    for (let i = n - 1; i > 0; i--) {
      const temp = a[0]; a[0] = a[i]; a[i] = temp;
      yield { type: 'swap', i: 0, j: i, lines: [9, 10, 11] };
      yield { type: 'markSorted', i, lines: [11] };
      yield* heapify(a, i, 0);
    }
  }

  function* heapify(a, n, i) {
    let largest = i;
    const left = 2 * i + 1;
    const right = 2 * i + 2;
    if (left < n) {
      yield { type: 'compare', i: left, j: largest, lines: [20] };
      if (a[left] > a[largest]) largest = left;
    }
    if (right < n) {
      yield { type: 'compare', i: right, j: largest, lines: [21] };
      if (a[right] > a[largest]) largest = right;
    }
    if (largest !== i) {
      const temp = a[i]; a[i] = a[largest]; a[largest] = temp;
      yield { type: 'swap', i, j: largest, lines: [23, 24, 25] };
      yield* heapify(a, n, largest);
    }
  }

  const GENERATORS = {
    bubble: bubbleSort,
    selection: selectionSort,
    insertion: insertionSort,
    merge: mergeSort,
    quick: quickSort,
    heap: heapSort,
  };

  return {
    JAVA_CODE,
    META,
    GENERATORS,
    ORDER: ['bubble', 'selection', 'insertion', 'merge', 'quick', 'heap'],
  };
});
