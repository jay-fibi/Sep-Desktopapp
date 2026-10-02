public class InsertionSort {

    /**
     * Sorts the given array in ascending order using insertion sort.
     * Time complexity: O(n^2) worst/average case, O(n) best case (already sorted).
     * Space complexity: O(1) — sorts in place.
     */
    public static void insertionSort(int[] arr) {
        if (arr == null) {
            return;
        }
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];          // element to be inserted
            int j = i - 1;
            // Shift elements greater than key one position to the right
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;          // insert key into its correct spot
        }
    }

    public static void main(String[] args) {
        int[] numbers = {12, 11, 13, 5, 6, 3, 45};

        System.out.print("Before sorting: ");
        printArray(numbers);

        insertionSort(numbers);

        System.out.print("After sorting:  ");
        printArray(numbers);
    }

    private static void printArray(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        System.out.println(sb);
    }
}
