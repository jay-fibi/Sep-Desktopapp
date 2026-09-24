/* Demo: count words and characters in a string */
#include <ctype.h>
#include <stdio.h>

void count(const char *text, int *words, int *chars) {
    *words = 0;
    *chars = 0;
    int in_word = 0;
    for (const char *p = text; *p != '\0'; ++p) {
        (*chars)++;
        if (isspace((unsigned char)*p)) {
            in_word = 0;
        } else if (!in_word) {
            in_word = 1;
            (*words)++;
        }
    }
}

int main(void) {
    const char *text = "the quick brown fox jumps over the lazy dog";
    int words, chars;
    count(text, &words, &chars);
    printf("Text: \"%s\"\n", text);
    printf("Words: %d\n", words);
    printf("Characters: %d\n", chars);
    return 0;
}
