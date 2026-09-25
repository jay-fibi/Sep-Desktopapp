import java.util.Arrays;
import java.util.Random;

/**
 * SortingAlgorithms — runnable versions of the six sorting algorithms
 * demonstrated step by step on the interactive webpage (index.html).
 *
 * Compile & run:
 *   javac SortingAlgorithms.java && java SortingAlgorithms
 *
 * The main method sorts a small sample array with every algorithm
 * (printing before/after), then races all six on a larger random array.
 */
public class SortingAlgorithms {

    // ==================== Bubble Sort ====================
    public static void bubbleSort(int[] arr) {
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
    }

    // ==================== Selection Sort ====================
    public static void selectionSort(int[] arr) {
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
    }

    // ==================== Insertion Sort ====================
    public static void insertionSort(int[] arr) {
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
    }

    // ==================== Merge Sort ====================
    public static void mergeSort(int[] arr, int left, int right) {
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
    }

    // Convenience overload so callers can just pass the array.
    public static void mergeSort(int[] arr) {
        mergeSort(arr, 0, arr.length - 1);
    }

    // ==================== Quick Sort ====================
    public static void quickSort(int[] arr, int low, int high) {
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
    }

    // Convenience overload so callers can just pass the array.
    public static void quickSort(int[] arr) {
        quickSort(arr, 0, arr.length - 1);
    }

    // ==================== Heap Sort ====================
    public static void heapSort(int[] arr) {
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
    }

    // ==================== Demo & micro-benchmark ====================

    private static boolean isSorted(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            if (arr[i - 1] > arr[i]) return false;
        }
        return true;
    }

    private interface Sorter {
        void sort(int[] arr);
    }

    public static void main(String[] args) {
        // LinkedHashMap-like pairing to keep a stable display order.
        String[] names = {
            "Bubble Sort", "Selection Sort", "Insertion Sort",
            "Merge Sort", "Quick Sort", "Heap Sort"
        };
        Sorter[] sorters = new Sorter[] {
            SortingAlgorithms::bubbleSort,
            SortingAlgorithms::selectionSort,
            SortingAlgorithms::insertionSort,
            SortingAlgorithms::mergeSort,
            SortingAlgorithms::quickSort,
            SortingAlgorithms::heapSort
        };

        // 1) Correctness demo on a small, readable sample.
        int[] sample = {38, 27, 43, 3, 9, 82, 10, 55, 21, 64};
        System.out.println("Sample array: " + Arrays.toString(sample));
        System.out.println("-".repeat(60));
        for (int a = 0; a < names.length; a++) {
            int[] copy = sample.clone();
            sorters[a].sort(copy);
            System.out.printf("%-15s -> %s  [%s]%n",
                    names[a], Arrays.toString(copy),
                    isSorted(copy) ? "sorted" : "FAILED");
        }

        // 2) Race all six on a larger random array.
        int size = 20_000;
        int[] data = new Random(42).ints(size, 0, 100_000).toArray();
        System.out.println("-".repeat(60));
        System.out.println("Race on " + size + " random ints:");
        for (int a = 0; a < names.length; a++) {
            int[] copy = data.clone();
            long start = System.nanoTime();
            sorters[a].sort(copy);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("%-15s %6d ms  [%s]%n",
                    names[a], elapsedMs,
                    isSorted(copy) ? "sorted" : "FAILED");
        }
    }
}
