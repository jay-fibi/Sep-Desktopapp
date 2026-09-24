import java.util.Scanner;

/**
 * Palindrome Number Checker.
 *
 * A palindrome number reads the same forwards and backwards,
 * e.g. 121, 12321, 7, and 0. Negative numbers are NOT palindromes
 * because of the leading minus sign (-121 reversed is 121-).
 *
 * Usage:
 *   java PalindromeNumber          -> interactive mode (prompts for input)
 *   java PalindromeNumber 121 42   -> checks the numbers given as arguments
 */
public class PalindromeNumber {

    /**
     * Checks whether the given number is a palindrome by reversing its
     * digits mathematically (no String conversion).
     *
     * @param number the number to check
     * @return true if the number reads the same forwards and backwards
     */
    public static boolean isPalindrome(long number) {
        if (number < 0) {
            return false; // negative numbers are not palindromes
        }

        long original = number;
        long reversed = 0;

        while (number > 0) {
            long digit = number % 10;          // take the last digit
            reversed = reversed * 10 + digit;  // append it to the reversed number
            number /= 10;                      // drop the last digit
        }

        return original == reversed;
    }

    /** Prints the result for a single number. */
    private static void printResult(long number) {
        if (isPalindrome(number)) {
            System.out.println(number + " is a palindrome number.");
        } else {
            System.out.println(number + " is not a palindrome number.");
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            // Command-line mode: check every argument that is a valid number.
            for (String arg : args) {
                try {
                    printResult(Long.parseLong(arg.trim()));
                } catch (NumberFormatException e) {
                    System.out.println("\"" + arg + "\" is not a valid number.");
                }
            }
        } else {
            // Interactive mode: keep asking until the user types 'q'.
            Scanner scanner = new Scanner(System.in);
            System.out.println("Palindrome Number Checker (type 'q' to quit)");
            while (true) {
                System.out.print("Enter a number: ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("q")) {
                    System.out.println("Goodbye!");
                    break;
                }
                try {
                    printResult(Long.parseLong(input));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a whole number.");
                }
            }
            scanner.close();
        }
    }
}
