import java.util.Arrays;

public class BubbleSort {
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            // Largest unsorted element "bubbles" to position n - 1 - i
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            // Already sorted: stop early
            if (!swapped) break;
        }
    }

    public static void main(String[] args) {
        int[] numbers = {5, 1, 4, 2, 8, 0, 3, 7, 6};
        System.out.println("Before: " + Arrays.toString(numbers));
        bubbleSort(numbers);
        System.out.println("After:  " + Arrays.toString(numbers));
    }
}
