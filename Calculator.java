import java.util.Scanner;

/**
 * Program: Simple Calculator.
 *
 * Performs the four basic arithmetic operations (+, -, *, /).
 *
 * Usage:
 *   java Calculator            interactive mode (type 'q' to quit)
 *   java Calculator 12 5       prints all four operations on 12 and 5
 *   java Calculator 12 + 5     prints a single operation
 */
public class Calculator {

    public static double add(double a, double b) { return a + b; }

    public static double subtract(double a, double b) { return a - b; }

    public static double multiply(double a, double b) { return a * b; }

    public static double divide(double a, double b) {
        if (b == 0) throw new ArithmeticException("Cannot divide by zero");
        return a / b;
    }

    /** Applies the given operator to two numbers. */
    public static double calculate(double a, double b, String operator) {
        switch (operator) {
            case "+": return add(a, b);
            case "-": return subtract(a, b);
            case "*": return multiply(a, b);
            case "/": return divide(a, b);
            default: throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            runInteractive();
        } else if (args.length == 2) {
            printAllOperations(args[0], args[1]);
        } else if (args.length == 3) {
            printSingleOperation(args[0], args[1], args[2]);
        } else {
            printUsage();
        }
    }

    /** java Calculator 12 5 */
    private static void printAllOperations(String first, String second) {
        double a;
        double b;
        try {
            a = Double.parseDouble(first);
            b = Double.parseDouble(second);
        } catch (NumberFormatException e) {
            System.out.println("Error: '" + first + "' and '" + second + "' must be numbers.");
            return;
        }
        System.out.println(expression(a, "+", b) + " = " + format(add(a, b)));
        System.out.println(expression(a, "-", b) + " = " + format(subtract(a, b)));
        System.out.println(expression(a, "*", b) + " = " + format(multiply(a, b)));
        try {
            System.out.println(expression(a, "/", b) + " = " + format(divide(a, b)));
        } catch (ArithmeticException e) {
            System.out.println(expression(a, "/", b) + " = Error: " + e.getMessage());
        }
    }

    /** java Calculator 12 + 5 */
    private static void printSingleOperation(String first, String operator, String second) {
        try {
            double a = Double.parseDouble(first);
            double b = Double.parseDouble(second);
            System.out.println(expression(a, operator, b) + " = " + format(calculate(a, b, operator)));
        } catch (NumberFormatException e) {
            System.out.println("Error: '" + first + "' and '" + second + "' must be numbers.");
        } catch (ArithmeticException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /** Interactive mode: keeps asking for number, operator, number until the user quits. */
    private static void runInteractive() {
        System.out.println("Simple Calculator - type 'q' at any prompt to quit.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String first = read(scanner, "First number: ");
            if (first == null || isQuit(first)) break;

            String operator = read(scanner, "Operator (+, -, *, /): ");
            if (operator == null || isQuit(operator)) break;

            String second = read(scanner, "Second number: ");
            if (second == null || isQuit(second)) break;

            try {
                double a = Double.parseDouble(first);
                double b = Double.parseDouble(second);
                System.out.println(expression(a, operator, b) + " = " + format(calculate(a, b, operator)));
            } catch (NumberFormatException e) {
                System.out.println("Error: '" + first + "' and '" + second + "' must be numbers.");
            } catch (ArithmeticException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
        System.out.println("Bye!");
        scanner.close();
    }

    /** Prints a prompt and reads one token; returns null when the input stream ends. */
    private static String read(Scanner scanner, String message) {
        System.out.print(message);
        return scanner.hasNext() ? scanner.next() : null;
    }

    private static boolean isQuit(String token) {
        return token.equalsIgnoreCase("q") || token.equalsIgnoreCase("quit");
    }

    /** Builds an expression string like "12 + 5". */
    private static String expression(double a, String operator, double b) {
        return format(a) + " " + operator + " " + format(b);
    }

    /** Formats whole numbers without a trailing ".0" (15 instead of 15.0). */
    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java Calculator            interactive mode");
        System.out.println("  java Calculator 12 5       all four operations on two numbers");
        System.out.println("  java Calculator 12 + 5     a single operation");
    }
}
