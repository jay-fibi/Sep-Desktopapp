# frozen_string_literal: true

require_relative 'test_helper'

# Exercises Calculator::Format, which decides how results are printed.
class FormatTest < CalculatorTestCase
  def format(value)
    Calculator::Format.number(value)
  end

  def test_whole_floats_lose_their_decimal_part
    assert_equal '10', format(10.0)
    assert_equal '-4', format(-4.0)
    assert_equal '0', format(0.0)
    assert_equal '0', format(-0.0)
  end

  def test_integers_are_printed_exactly
    assert_equal '42', format(42)
    assert_equal '1000000000000000000000000000000', format(10**30)
  end

  def test_fractions_keep_twelve_significant_digits
    assert_equal '0.5', format(0.5)
    assert_equal '0.3', format(0.1 + 0.2)
    assert_equal '0.333333333333', format(1.0 / 3)
    assert_equal '3.14159265359', format(Math::PI)
  end

  def test_small_numbers_use_scientific_notation_only_when_needed
    assert_equal '0.001', format(0.001)
    assert_equal '0.000123456789012', format(0.000123456789012345)
  end

  def test_very_large_values_use_scientific_notation
    assert_equal '1e+16', format(1e16)
    assert_equal '1e+20', format(1e20)
  end

  def test_long_floats_are_rounded_for_readability
    assert_equal '123456789012345', format(123_456_789_012_345.0)
    assert_equal '1.23456789012e+20', format(123_456_789_012_345_000_000.0)
  end

  def test_other_numeric_types_are_rendered_as_floats
    assert_equal '0.5', format(Rational(1, 2))
  end

  # The engine never produces these, but the formatter never raises either.
  def test_non_finite_values_use_rubys_own_spelling
    assert_equal 'NaN', format(Float::NAN)
    assert_equal 'Infinity', format(Float::INFINITY)
    assert_equal '-Infinity', format(-Float::INFINITY)
  end
end
