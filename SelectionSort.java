import java.util.Arrays;

/**
 * A simple implementation of the Selection Sort algorithm in Java.
 *
 * How it works:
 *   - Repeatedly find the smallest element in the unsorted portion of the array
 *     and swap it into the next position of the sorted portion.
 *
 * Time complexity:  O(n^2) in all cases (best, average, worst)
 * Space complexity: O(1)  (in-place, sorts without extra arrays)
 * Stable:           No (a basic implementation is not stable)
 */
public class SelectionSort {

    /**
     * Sorts the given array in ascending order, in place.
     *
     * @param array the array to sort (may be null or empty)
     */
    public static void selectionSort(int[] array) {
        if (array == null || array.length < 2) {
            return; // nothing to sort
        }

        int n = array.length;
        for (int i = 0; i < n - 1; i++) {
            // Assume the current index holds the smallest value.
            int minIndex = i;

            // Scan the unsorted part to find the true minimum.
            for (int j = i + 1; j < n; j++) {
                if (array[j] < array[minIndex]) {
                    minIndex = j;
                }
            }

            // Swap the found minimum into position i (only if needed).
            if (minIndex != i) {
                int temp = array[i];
                array[i] = array[minIndex];
                array[minIndex] = temp;
            }
        }
    }

    public static void main(String[] args) {
        int[] data = {64, 25, 12, 22, 11, 90, 1};

        System.out.println("Unsorted array: " + Arrays.toString(data));
        selectionSort(data);
        System.out.println("Sorted array:   " + Arrays.toString(data));

        // A few more quick checks.
        int[] single = {42};
        selectionSort(single);
        System.out.println("Single element: " + Arrays.toString(single));

        int[] empty = {};
        selectionSort(empty);
        System.out.println("Empty array:    " + Arrays.toString(empty));

        int[] alreadySorted = {1, 2, 3, 4, 5};
        selectionSort(alreadySorted);
        System.out.println("Already sorted: " + Arrays.toString(alreadySorted));
    }
}
