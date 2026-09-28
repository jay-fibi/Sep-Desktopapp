import java.util.Arrays;

/**
 * MergeSort - A classic divide-and-conquer sorting algorithm.
 *
 * How it works:
 *   1. Divide   : Split the array into two halves.
 *   2. Conquer  : Recursively sort each half.
 *   3. Combine  : Merge the two sorted halves back together.
 *
 * Time complexity:  O(n log n) in all cases (best, average, worst)
 * Space complexity: O(n) for the temporary arrays used during merging
 */
public class MergeSort {

    /**
     * Public entry point: sorts the given array in ascending order.
     *
     * @param arr the array to sort (modified in place)
     */
    public static void sort(int[] arr) {
        if (arr == null || arr.length < 2) {
            return; // Already sorted (empty or single element)
        }
        mergeSort(arr, 0, arr.length - 1);
    }

    /**
     * Recursively sorts the sub-array arr[left..right].
     */
    private static void mergeSort(int[] arr, int left, int right) {
        if (left >= right) {
            return; // Base case: sub-array of size 1 is already sorted
        }

        // Find the middle point (avoids overflow compared to (left + right) / 2)
        int mid = left + (right - left) / 2;

        // Sort first and second halves
        mergeSort(arr, left, mid);
        mergeSort(arr, mid + 1, right);

        // Merge the sorted halves
        merge(arr, left, mid, right);
    }

    /**
     * Merges two sorted sub-arrays: arr[left..mid] and arr[mid+1..right].
     */
    private static void merge(int[] arr, int left, int mid, int right) {
        // Sizes of the two sub-arrays
        int leftSize = mid - left + 1;
        int rightSize = right - mid;

        // Temporary arrays
        int[] leftArr = new int[leftSize];
        int[] rightArr = new int[rightSize];

        // Copy data into the temporary arrays
        System.arraycopy(arr, left, leftArr, 0, leftSize);
        System.arraycopy(arr, mid + 1, rightArr, 0, rightSize);

        // Merge the temp arrays back into arr[left..right]
        int i = 0;          // Index into leftArr
        int j = 0;          // Index into rightArr
        int k = left;       // Index into the merged position of arr

        while (i < leftSize && j < rightSize) {
            if (leftArr[i] <= rightArr[j]) {
                arr[k++] = leftArr[i++];
            } else {
                arr[k++] = rightArr[j++];
            }
        }

        // Copy any remaining elements of leftArr
        while (i < leftSize) {
            arr[k++] = leftArr[i++];
        }

        // Copy any remaining elements of rightArr
        while (j < rightSize) {
            arr[k++] = rightArr[j++];
        }
    }

    public static void main(String[] args) {
        int[] numbers = {38, 27, 43, 3, 9, 82, 10};

        System.out.println("Original array: " + Arrays.toString(numbers));
        sort(numbers);
        System.out.println("Sorted array:   " + Arrays.toString(numbers));

        // A few extra test cases
        int[][] tests = {
            {},                           // empty
            {42},                         // single element
            {5, 4, 3, 2, 1},              // reverse sorted
            {1, 2, 3, 4, 5},              // already sorted
            {7, -3, 0, 7, -3, 100, -50}   // duplicates and negatives
        };

        System.out.println("\n--- Additional test cases ---");
        for (int[] test : tests) {
            int[] copy = Arrays.copyOf(test, test.length);
            sort(copy);
            System.out.println(Arrays.toString(test) + "  ->  " + Arrays.toString(copy));
        }
    }
}
