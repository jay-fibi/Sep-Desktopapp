// Demo: string utility functions

function reverseString(str) {
  return str.split("").reverse().join("");
}

function isPalindrome(str) {
  const clean = str.toLowerCase().replace(/[^a-z0-9]/g, "");
  return clean === reverseString(clean);
}

function capitalize(str) {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

console.log(reverseString("hello"));        // olleh
console.log(isPalindrome("Racecar"));       // true
console.log(capitalize("demo file"));       // Demo file

module.exports = { reverseString, isPalindrome, capitalize };
