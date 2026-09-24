import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Fibonacci {
    /* BigInteger keeps the sequence exact for any n, like Python's ints. */
    public static List<BigInteger> fibonacci(int n) {
        List<BigInteger> sequence = new ArrayList<>();
        if (n <= 0) {
            return sequence;
        }
        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;
        sequence.add(a);
        for (int i = 1; i < n; i++) {
            BigInteger next = a.add(b);
            a = b;
            b = next;
            sequence.add(a);
        }
        return sequence;
    }

    public static void main(String[] args) {
        int count = 10;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException error) {
                System.out.println("Invalid input '" + args[0] + "', using default of 10.");
            }
        }
        System.out.println("First " + count + " Fibonacci numbers: " + fibonacci(count));
    }
}
