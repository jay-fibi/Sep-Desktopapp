/*
 * star_patterns.c
 *
 * A menu-driven C program that prints various star (*) patterns.
 * Patterns included:
 *   1. Right-angled triangle
 *   2. Inverted right-angled triangle
 *   3. Pyramid
 *   4. Inverted pyramid
 *   5. Diamond
 *
 * Compile: gcc -o star_patterns star_patterns.c
 * Run:     ./star_patterns
 */

#include <stdio.h>

/* 1. Right-angled triangle
 * *
 * * *
 * * * *
 */
void print_right_triangle(int rows)
{
    for (int i = 1; i <= rows; i++) {
        for (int j = 1; j <= i; j++) {
            printf("* ");
        }
        printf("\n");
    }
}

/* 2. Inverted right-angled triangle
 * * * *
 * * *
 * *
 */
void print_inverted_triangle(int rows)
{
    for (int i = rows; i >= 1; i--) {
        for (int j = 1; j <= i; j++) {
            printf("* ");
        }
        printf("\n");
    }
}

/* 3. Pyramid
 *    *
 *   * *
 *  * * *
 */
void print_pyramid(int rows)
{
    for (int i = 1; i <= rows; i++) {
        for (int space = 1; space <= rows - i; space++) {
            printf("  ");
        }
        for (int j = 1; j <= 2 * i - 1; j++) {
            printf("* ");
        }
        printf("\n");
    }
}

/* 4. Inverted pyramid
 * * * * *
 *  * * *
 *   *
 */
void print_inverted_pyramid(int rows)
{
    for (int i = rows; i >= 1; i--) {
        for (int space = 1; space <= rows - i; space++) {
            printf("  ");
        }
        for (int j = 1; j <= 2 * i - 1; j++) {
            printf("* ");
        }
        printf("\n");
    }
}

/* 5. Diamond (pyramid + inverted pyramid) */
void print_diamond(int rows)
{
    /* Upper half */
    for (int i = 1; i <= rows; i++) {
        for (int space = 1; space <= rows - i; space++) {
            printf("  ");
        }
        for (int j = 1; j <= 2 * i - 1; j++) {
            printf("* ");
        }
        printf("\n");
    }
    /* Lower half */
    for (int i = rows - 1; i >= 1; i--) {
        for (int space = 1; space <= rows - i; space++) {
            printf("  ");
        }
        for (int j = 1; j <= 2 * i - 1; j++) {
            printf("* ");
        }
        printf("\n");
    }
}

int main(void)
{
    int choice, rows;

    printf("===== Star Pattern Generator =====\n");
    printf("1. Right-angled triangle\n");
    printf("2. Inverted right-angled triangle\n");
    printf("3. Pyramid\n");
    printf("4. Inverted pyramid\n");
    printf("5. Diamond\n");
    printf("Enter your choice (1-5): ");

    if (scanf("%d", &choice) != 1 || choice < 1 || choice > 5) {
        printf("Invalid choice. Please run again and enter 1-5.\n");
        return 1;
    }

    printf("Enter number of rows: ");
    if (scanf("%d", &rows) != 1 || rows <= 0) {
        printf("Invalid input. Number of rows must be a positive integer.\n");
        return 1;
    }

    printf("\n");
    switch (choice) {
        case 1: print_right_triangle(rows);    break;
        case 2: print_inverted_triangle(rows); break;
        case 3: print_pyramid(rows);           break;
        case 4: print_inverted_pyramid(rows);  break;
        case 5: print_diamond(rows);           break;
    }

    return 0;
}
