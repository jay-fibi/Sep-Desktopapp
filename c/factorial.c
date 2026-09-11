/* Program 2: Factorial Calculator.
 *
 * Computes the factorial of a number (default 5).
 *
 * Build: gcc factorial.c -o factorial
 * Run:   ./factorial 6
 */

#include <stdio.h>
#include <stdlib.h>

long factorial(int n) {
    long result = 1;
    for (int i = 2; i <= n; i++) {
        result *= i;
    }
    return result;
}

int main(int argc, char *argv[]) {
    int number = 5;
    if (argc > 1) {
        number = atoi(argv[1]);
    }
    if (number < 0) {
        printf("Error: factorial is not defined for negative numbers.\n");
        return 1;
    }
    printf("%d! = %ld\n", number, factorial(number));
    return 0;
}
