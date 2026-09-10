// Demo: temperature conversion utilities
package main

import "fmt"

func celsiusToFahrenheit(c float64) float64 {
	return c*9/5 + 32
}

func fahrenheitToCelsius(f float64) float64 {
	return (f - 32) * 5 / 9
}

func main() {
	fmt.Printf("25°C = %.1f°F\n", celsiusToFahrenheit(25))
	fmt.Printf("98.6°F = %.1f°C\n", fahrenheitToCelsius(98.6))
}
