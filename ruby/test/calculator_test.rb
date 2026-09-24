# frozen_string_literal: true

require_relative 'test_helper'

# Exercises Calculator.evaluate and the arithmetic it performs.
class CalculatorTest < CalculatorTestCase
  def test_version_is_a_string
    assert_kind_of String, Calculator::VERSION
  end

  def test_evaluates_a_single_expression
    assert_calculates 7, '1 + 2 * 3'
  end

  def test_evaluate_returns_the_value_of_the_last_statement
    assert_equal 12, Calculator.evaluate('1 + 2; 3 * 4')
  end

  def test_evaluate_accepts_a_seeded_scope
    assert_equal 15, Calculator.evaluate('x * 3', variables: { 'x' => 5 })
  end

  def test_evaluate_leaves_the_seeded_hash_alone
    seed = { 'x' => 5 }
    Calculator.evaluate('x = 99', variables: seed)

    assert_equal({ 'x' => 5 }, seed)
  end

  def test_blank_input_is_rejected
    assert_calculator_error Calculator::ParseError, 'unexpected end of expression', '   '
  end

  def test_addition_and_subtraction
    assert_calculates 3, '1 + 2'
    assert_calculates(-1, '1 - 2')
    assert_calculates 0, '1 - 2 + 1'
  end

  def test_multiplication
    assert_calculates 42, '6 * 7'
  end

  # Division always produces a Float, unlike Ruby's Integer#/.
  def test_division_produces_a_float
    assert_calculates '2.5', '10 / 4'
    assert_calculates '0.333333333333', '1 / 3'
    assert_calculates '2', '4 / 2'
  end

  def test_modulo
    assert_calculates 1, '7 % 3'
    assert_calculates 0, '9 % 3'
    assert_calculates '1.5', '5.5 % 2'
  end

  # Ruby's % always has the sign of the divisor.
  def test_modulo_sign_follows_the_divisor
    assert_calculates 2, '-7 % 3'
    assert_calculates(-2, '7 % -3')
  end

  def test_precedence
    assert_calculates 14, '2 + 3 * 4'
    assert_calculates 20, '(2 + 3) * 4'
    assert_calculates 10, '2 * 3 + 4'
    assert_calculates '1.5', '3 / 2 + 0'
  end

  def test_nested_parentheses
    assert_calculates 21, '((1 + 2) * (3 + 4))'
  end

  def test_power_is_right_associative
    assert_calculates 512, '2 ** 3 ** 2'
  end

  def test_power_binds_tighter_than_unary_minus
    assert_calculates(-4, '-2 ** 2')
    assert_calculates 4, '(-2) ** 2'
  end

  def test_unary_operators
    assert_calculates 3, '--3'
    assert_calculates 3, '+3'
    assert_calculates(-3, '-(1 + 2)')
    assert_calculates '0.5', '2 ** -1'
    assert_calculates(-8, '-2 ** 3')
  end

  def test_whole_numbers_keep_full_precision
    assert_equal 10**30, Calculator.evaluate('10 ** 30')
    assert_calculates '1000000000000000000000000000000', '10 ** 30'
  end

  def test_decimal_and_exponent_literals
    assert_calculates '0.5', '.5'
    assert_calculates 1000, '1e3'
    assert_calculates '0.001', '1e-3'
    assert_calculates '1', '1.'
  end

  def test_floating_point_noise_is_hidden
    assert_calculates '0.3', '0.1 + 0.2'
  end

  def test_statements_and_variables
    assert_calculates 1024, 'x = 2; y = x ** 10; y'
  end

  def test_functions_can_be_nested
    assert_calculates 5, 'sqrt(pow(3, 2) + 4 ** 2)'
  end

  def test_whitespace_is_optional
    assert_calculates 7, '1+2*3'
    assert_calculates 7, "1 +\n2 * 3"
  end

  def test_division_by_zero
    assert_calculator_error Calculator::EvaluationError, 'Cannot divide by zero', '1 / 0'
    assert_calculator_error Calculator::EvaluationError, 'Cannot divide by zero', '1 / (2 - 2)'
  end

  def test_modulo_by_zero
    assert_calculator_error Calculator::EvaluationError,
                            'Cannot take a remainder modulo zero', '1 % 0'
  end

  def test_complex_results_are_rejected
    assert_calculator_error Calculator::EvaluationError,
                            'result is not a real number', '(-8) ** (1 / 3)'
  end

  def test_results_that_overflow_are_rejected
    assert_calculator_error Calculator::EvaluationError,
                            'result is outside the supported numeric range', '1e308 * 10'
  end

  def test_unknown_variable
    assert_calculator_error Calculator::EvaluationError, "unknown variable 'x'", 'x + 1'
  end

  def test_unknown_function
    assert_calculator_error Calculator::EvaluationError, "unknown function 'nope'", 'nope(1)'
  end
end
