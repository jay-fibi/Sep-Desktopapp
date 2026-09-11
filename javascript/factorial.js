// Program 2: Factorial Calculator.
//
// Computes the factorial of a number (default 5).
//
// Run: node factorial.js 6

function factorial(n) {
  if (n < 0) {
    throw new Error("Factorial is not defined for negative numbers.");
  }
  let result = 1;
  for (let i = 2; i <= n; i++) {
    result *= i;
  }
  return result;
}

const number = process.argv[2] !== undefined ? Number(process.argv[2]) : 5;

try {
  console.log(`${number}! = ${factorial(number)}`);
} catch (err) {
  console.log(`Error: ${err.message}`);
}
