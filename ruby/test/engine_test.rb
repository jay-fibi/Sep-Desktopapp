# frozen_string_literal: true

require_relative 'test_helper'

# Exercises Calculator::Engine: functions, constants, variables and the
# guards that keep results inside the supported numeric range.
class EngineTest < CalculatorTestCase
  def engine(**options)
    Calculator::Engine.new(**options)
  end

  def test_ans_starts_at_zero
    assert_equal 0, engine.evaluate('ans')
  end

  def test_ans_holds_the_previous_result
    calculator = engine

    assert_equal 2, calculator.evaluate('1 + 1')
    assert_equal 6, calculator.evaluate('ans * 3')
  end

  def test_ans_holds_the_last_statement_of_a_program
    calculator = engine
    calculator.evaluate('1 + 1; 5 * 5')

    assert_equal 25, calculator.evaluate('ans')
  end

  def test_variables_are_remembered_between_evaluations
    calculator = engine

    assert_equal 3, calculator.evaluate('radius = 3')
    assert_equal 9, calculator.evaluate('radius ** 2')
    assert_equal({ 'ans' => 9, 'radius' => 3 }, calculator.variables)
  end

  def test_assignment_evaluates_to_the_assigned_value
    assert_equal 12, engine.evaluate('x = 3 * 4')
  end

  def test_variables_can_shadow_constants
    calculator = engine
    calculator.evaluate('pi = 3')

    assert_equal 3, calculator.evaluate('pi')
  end

  def test_constants
    assert_in_delta Math::PI, engine.evaluate('pi')
    assert_in_delta Math::E, engine.evaluate('e')
    assert_in_delta Math::PI * 2, engine.evaluate('tau')
  end

  def test_reset_clears_variables
    calculator = engine
    calculator.evaluate('x = 1')

    assert_equal calculator, calculator.reset
    assert_equal({ 'ans' => 0 }, calculator.variables)
  end

  def test_name_lookup_is_case_sensitive
    assert_calculator_error Calculator::EvaluationError, "unknown variable 'PI'", 'PI'
  end

  def test_basic_functions
    assert_calculates 5, 'sqrt(25)'
    assert_calculates 2, 'cbrt(8)'
    assert_calculates(-2, 'cbrt(-8)')
    assert_calculates 7, 'abs(-7)'
    assert_calculates(-1, 'sign(-3)')
    assert_calculates 0, 'sign(0)'
    assert_calculates 1, 'sign(2.5)'
  end

  def test_rounding_functions
    assert_calculates '2.57', 'round(2.567, 2)'
    assert_calculates 3, 'round(2.567)'
    assert_calculates 3, 'ceil(2.1)'
    assert_calculates 2, 'floor(2.9)'
    assert_calculates 3, 'truncate(3.9)'
    assert_calculates(-3, 'truncate(-3.9)')
  end

  def test_logarithm_functions
    assert_calculates 3, 'log(8, 2)'
    assert_calculates 1, 'log(e)'
    assert_calculates 3, 'log10(1000)'
    assert_calculates 10, 'log2(1024)'
    assert_calculates 1, 'exp(0)'
  end

  def test_aggregate_functions
    assert_calculates 3, 'min(3)'
    assert_calculates 1, 'min(3, 1, 2)'
    assert_calculates 3, 'max(3, 1, 2)'
    assert_calculates 4, 'pow(2, 2)'
  end

  def test_trigonometry
    assert_calculates 1, 'cos(0)'
    assert_calculates 0, 'sin(0)'
    assert_calculates 0, 'tan(0)'
    assert_calculates 5, 'hypot(3, 4)'
    assert_calculates '0.785398163397', 'atan2(1, 1)'
  end

  def test_angle_conversions
    assert_calculates 180, 'degrees(pi)'
    assert_calculates '3.14159265359', 'radians(180)'
  end

  def test_factorial
    assert_calculates 120, 'fact(5)'
    assert_calculates 120, 'fact(5.0)'
    assert_calculates 1, 'fact(0)'
    assert_calculates 30, 'fact(30) / fact(29)'
  end

  def test_factorial_rejects_bad_arguments
    assert_calculator_error Calculator::EvaluationError, 'fact expects a whole number', 'fact(2.5)'
    assert_calculator_error Calculator::EvaluationError, 'fact expects a non-negative number', 'fact(-1)'
    assert_calculator_error Calculator::EvaluationError, 'fact is limited to n <= 1000', 'fact(1001)'
  end

  def test_argument_count_is_checked
    assert_calculator_error Calculator::EvaluationError,
                            'sqrt expects 1 argument(s) but got 0', 'sqrt()'
    assert_calculator_error Calculator::EvaluationError,
                            'sqrt expects 1 argument(s) but got 2', 'sqrt(1, 2)'
    assert_calculator_error Calculator::EvaluationError,
                            'round expects 1 to 2 argument(s) but got 3', 'round(1, 2, 3)'
    assert_calculator_error Calculator::EvaluationError,
                            'min expects 1 or more argument(s) but got 0', 'min()'
  end

  def test_domain_errors_are_reported_clearly
    assert_calculator_error Calculator::EvaluationError,
                            'sqrt is not defined for these arguments', 'sqrt(-1)'
    assert_calculator_error Calculator::EvaluationError,
                            'log is not defined for these arguments', 'log(-1)'
    assert_calculator_error Calculator::EvaluationError,
                            'asin is not defined for these arguments', 'asin(2)'
  end

  def test_rational_results_are_returned_as_floats
    assert_equal 0.5, engine.evaluate('2 ** -1')
    assert_instance_of Float, engine.evaluate('2 ** -1')
  end

  def test_exponents_of_whole_numbers_are_capped
    assert_calculator_error Calculator::EvaluationError,
                            'exponent is limited to 1000000 for whole-number bases', '2 ** 10 ** 9'
    assert_equal 2**1000, engine.evaluate('2 ** 1000')
  end

  def test_zero_to_a_negative_power
    assert_calculator_error Calculator::EvaluationError, 'Cannot divide by zero', '0 ** -1'
  end

  def test_seeded_variables_must_still_be_finite
    [Float::INFINITY, Float::NAN].each do |value|
      error = assert_raises(Calculator::EvaluationError) do
        Calculator.evaluate('x', variables: { 'x' => value })
      end

      assert_equal 'result is outside the supported numeric range', error.message
    end
  end

  def test_seeded_variables_are_used_for_arithmetic
    assert_equal 14.0, Calculator.evaluate('x * 2 + 4', variables: { 'x' => 5 })
  end
end
