#include <stdio.h>

int main(void) {
    double a, b;
    char op;

    printf("Simple Calculator\n");
    printf("Enter expression (e.g. 3 + 4): ");
    if (scanf("%lf %c %lf", &a, &op, &b) != 3) {
        fprintf(stderr, "Invalid input.\n");
        return 1;
    }

    switch (op) {
        case '+':
            printf("Result: %g\n", a + b);
            break;
        case '-':
            printf("Result: %g\n", a - b);
            break;
        case '*':
            printf("Result: %g\n", a * b);
            break;
        case '/':
            if (b == 0) {
                fprintf(stderr, "Error: division by zero.\n");
                return 1;
            }
            printf("Result: %g\n", a / b);
            break;
        default:
            fprintf(stderr, "Unknown operator '%c'.\n", op);
            return 1;
    }

    return 0;
}
