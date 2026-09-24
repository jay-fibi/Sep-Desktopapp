// Demo: simple matrix operations
#include <iostream>
#include <vector>

using Matrix = std::vector<std::vector<int>>;

Matrix transpose(const Matrix& m) {
    Matrix result(m[0].size(), std::vector<int>(m.size()));
    for (size_t i = 0; i < m.size(); ++i)
        for (size_t j = 0; j < m[0].size(); ++j)
            result[j][i] = m[i][j];
    return result;
}

Matrix multiply(const Matrix& a, const Matrix& b) {
    Matrix result(a.size(), std::vector<int>(b[0].size(), 0));
    for (size_t i = 0; i < a.size(); ++i)
        for (size_t j = 0; j < b[0].size(); ++j)
            for (size_t k = 0; k < b.size(); ++k)
                result[i][j] += a[i][k] * b[k][j];
    return result;
}

void print(const Matrix& m) {
    for (const auto& row : m) {
        for (int value : row) std::cout << value << ' ';
        std::cout << '\n';
    }
}

int main() {
    Matrix a = {{1, 2}, {3, 4}};
    Matrix b = {{5, 6}, {7, 8}};
    std::cout << "A * B =\n";
    print(multiply(a, b));
    std::cout << "Transpose of A =\n";
    print(transpose(a));
}
