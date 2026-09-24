import java.util.Locale;
import java.util.Scanner;

public class Calculator {
    public static int add(int a, int b) { return a + b; }
    public static int subtract(int a, int b) { return a - b; }
    public static int multiply(int a, int b) { return a * b; }
    public static double divide(int a, int b) {
        if (b == 0) throw new ArithmeticException("Cannot divide by zero");
        return (double) a / b;
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            scanner.useLocale(Locale.US);
            System.out.println("Simple Calculator (+, -, *, /)");

            System.out.print("Enter first number: ");
            if (!scanner.hasNextDouble()) {
                System.out.println("Error: Please enter a valid number.");
                return;
            }
            double first = scanner.nextDouble();

            System.out.print("Enter operator (+, -, *, /): ");
            if (!scanner.hasNext()) {
                System.out.println("Error: An operator is required.");
                return;
            }
            String operator = scanner.next();

            System.out.print("Enter second number: ");
            if (!scanner.hasNextDouble()) {
                System.out.println("Error: Please enter a valid number.");
                return;
            }
            double second = scanner.nextDouble();

            if (!Double.isFinite(first) || !Double.isFinite(second)) {
                System.out.println("Error: Numbers must be finite.");
                return;
            }

            double result;
            switch (operator) {
                case "+":
                    result = first + second;
                    break;
                case "-":
                    result = first - second;
                    break;
                case "*":
                    result = first * second;
                    break;
                case "/":
                    if (second == 0) {
                        System.out.println("Error: Cannot divide by zero.");
                        return;
                    }
                    result = first / second;
                    break;
                default:
                    System.out.println("Error: Unsupported operator. Use +, -, *, or /.");
                    return;
            }

            if (!Double.isFinite(result)) {
                System.out.println("Error: Result is outside the supported numeric range.");
                return;
            }
            System.out.println("Result: " + result);
        }
    }
}