import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Bucket sort program.
 *
 * Bucket sort distributes elements into a number of "buckets" that each cover
 * a slice of the value range, sorts every bucket individually (here with
 * insertion sort), and concatenates the buckets back together. It performs
 * best (close to O(n)) when the input is uniformly distributed across its
 * range; badly skewed data degrades it toward insertion sort's O(n^2).
 */
public class BucketSort {

    /**
     * Sorts an array of doubles in place using bucket sort.
     * Handles negative values, duplicates, empty arrays, and single elements.
     */
    public static void sort(double[] arr) {
        if (arr == null || arr.length < 2) {
            return; // nothing to sort
        }

        // 1. Find the range of the input.
        double min = arr[0];
        double max = arr[0];
        for (double value : arr) {
            if (value < min) min = value;
            if (value > max) max = value;
        }
        if (min == max) {
            return; // all elements are identical
        }

        // 2. Create the buckets (sqrt(n) is a common heuristic).
        int bucketCount = (int) Math.ceil(Math.sqrt(arr.length));
        List<List<Double>> buckets = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            buckets.add(new ArrayList<>());
        }

        // 3. Scatter: map each value onto the bucket covering its slice of [min, max].
        double range = max - min;
        for (double value : arr) {
            int index = (int) ((value - min) / range * (bucketCount - 1));
            index = Math.max(0, Math.min(index, bucketCount - 1)); // guard against rounding
            buckets.get(index).add(value);
        }

        // 4. Sort each bucket and gather the results back into the array.
        int position = 0;
        for (List<Double> bucket : buckets) {
            if (bucket.isEmpty()) {
                continue;
            }
            double[] values = new double[bucket.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = bucket.get(i);
            }
            insertionSort(values);
            for (double value : values) {
                arr[position++] = value;
            }
        }
    }

    /** Sorts a small array in place with insertion sort. */
    private static void insertionSort(double[] arr) {
        for (int i = 1; i < arr.length; i++) {
            double key = arr[i];
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    public static void main(String[] args) {
        double[][] examples = {
            {0.42, 0.32, 0.33, 0.52, 0.37, 0.47, 0.51},  // classic [0, 1) input
            {29, 25, 3, 49, 9, 37, 21, 43},              // whole numbers
            {-5.2, 3.14, 0.0, -1.5, 2.71, -5.2, 99.9},   // negatives and duplicates
            {7},                                         // single element
            {}                                          // empty array
        };

        for (double[] original : examples) {
            double[] arr = Arrays.copyOf(original, original.length);
            sort(arr);
            System.out.printf("Original: %s%nSorted:   %s%n%n",
                    Arrays.toString(original), Arrays.toString(arr));
        }

        // Sanity check against the standard library on a larger random array.
        double[] random = new Random(42).doubles(1000, -100, 100).toArray();
        double[] expected = Arrays.copyOf(random, random.length);
        Arrays.sort(expected);
        sort(random);
        System.out.println("1000-element array sorted correctly: "
                + Arrays.equals(random, expected));
    }
}
