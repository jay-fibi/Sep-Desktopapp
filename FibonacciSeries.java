import java.util.Scanner;

/**
 * Program to print the Fibonacci series.
 * Fibonacci series: each number is the sum of the two preceding ones,
 * starting from 0 and 1. e.g. 0, 1, 1, 2, 3, 5, 8, 13, 21, ...
 */
public class FibonacciSeries {

    /**
     * Returns the nth Fibonacci number using iteration.
     * Uses long to support larger values of n.
     */
    public static long fibonacci(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        long first = 0, second = 1;
        for (int i = 0; i < n; i++) {
            long next = first + second;
            first = second;
            second = next;
        }
        return first;
    }

    /**
     * Prints the first n terms of the Fibonacci series.
     */
    public static void printSeries(int n) {
        long first = 0, second = 1;
        System.out.print("Fibonacci series (first " + n + " terms): ");
        for (int i = 0; i < n; i++) {
            System.out.print(first);
            if (i < n - 1) {
                System.out.print(", ");
            }
            long next = first + second;
            first = second;
            second = next;
        }
        System.out.println();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of terms: ");

        if (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a whole number.");
            scanner.close();
            return;
        }

        int terms = scanner.nextInt();
        if (terms <= 0) {
            System.out.println("Please enter a positive number of terms.");
        } else {
            printSeries(terms);
            System.out.println("The " + terms + "th Fibonacci number is: "
                    + fibonacci(terms - 1));
        }

        scanner.close();
    }
}
