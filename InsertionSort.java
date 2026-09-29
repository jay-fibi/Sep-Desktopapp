import java.util.Arrays;

public class InsertionSort {
    /* Sorts the array in place using insertion sort: each element is inserted
     * into its correct position within the already-sorted left portion. */
    public static void insertionSort(int[] array) {
        for (int i = 1; i < array.length; i++) {
            int key = array[i];
            int j = i - 1;
            while (j >= 0 && array[j] > key) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
    }

    public static void main(String[] args) {
        int[] numbers;
        if (args.length > 0) {
            numbers = new int[args.length];
            for (int i = 0; i < args.length; i++) {
                try {
                    numbers[i] = Integer.parseInt(args[i]);
                } catch (NumberFormatException error) {
                    System.out.println("Invalid input '" + args[i] + "', expected an integer.");
                    return;
                }
            }
        } else {
            numbers = new int[] {12, 11, 13, 5, 6};
        }
        System.out.println("Before: " + Arrays.toString(numbers));
        insertionSort(numbers);
        System.out.println("After:  " + Arrays.toString(numbers));
    }
}
