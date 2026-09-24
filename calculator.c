#include <stdio.h>
#include <stdlib.h>

double add(double a, double b) {
    return a + b;
}

double subtract(double a, double b) {
    return a - b;
}

double multiply(double a, double b) {
    return a * b;
}

int divide(double a, double b, double *result) {
    if (b == 0.0) {
        return 0; // Error: division by zero
    }
    *result = a / b;
    return 1; // Success
}

int main(void) {
    char op;
    double num1, num2, result;

    printf("=================================\n");
    printf("        Simple C Calculator      \n");
    printf("=================================\n");
    printf("Select an operator (+, -, *, /) or 'q' to quit:\n");

    while (1) {
        printf("\nEnter operator (+, -, *, /, q): ");
        if (scanf(" %c", &op) != 1) {
            break;
        }

        if (op == 'q' || op == 'Q') {
            printf("Exiting calculator. Goodbye!\n");
            break;
        }

        if (op != '+' && op != '-' && op != '*' && op != '/') {
            printf("Error: Invalid operator '%c'. Please use +, -, *, /, or q.\n", op);
            continue;
        }

        printf("Enter first number: ");
        if (scanf("%lf", &num1) != 1) {
            printf("Error: Invalid number input.\n");
            // Clear input buffer
            while (getchar() != '\n');
            continue;
        }

        printf("Enter second number: ");
        if (scanf("%lf", &num2) != 1) {
            printf("Error: Invalid number input.\n");
            // Clear input buffer
            while (getchar() != '\n');
            continue;
        }

        switch (op) {
            case '+':
                result = add(num1, num2);
                printf("Result: %.4f + %.4f = %.4f\n", num1, num2, result);
                break;
            case '-':
                result = subtract(num1, num2);
                printf("Result: %.4f - %.4f = %.4f\n", num1, num2, result);
                break;
            case '*':
                result = multiply(num1, num2);
                printf("Result: %.4f * %.4f = %.4f\n", num1, num2, result);
                break;
            case '/':
                if (divide(num1, num2, &result)) {
                    printf("Result: %.4f / %.4f = %.4f\n", num1, num2, result);
                } else {
                    printf("Error: Division by zero is not allowed.\n");
                }
                break;
            default:
                printf("Error: Unknown operator.\n");
                break;
        }
    }

    return 0;
}
