# frozen_string_literal: true

require 'minitest/autorun'
require_relative '../lib/calculator'

# Helpers shared by every calculator test.
module CalculatorTestHelpers
  # Evaluates +source+ and returns the formatted result, so the tests can
  # compare the strings users actually see.
  def calculate(source, **options)
    Calculator::Format.number(Calculator.evaluate(source, **options))
  end

  # Asserts that +source+ evaluates to +expected+.
  #
  # A String expectation is compared against the formatted result, so tests can
  # pin down exactly what users see; a Numeric expectation is compared against
  # the raw value.
  def assert_calculates(expected, source)
    actual = expected.is_a?(String) ? calculate(source) : Calculator.evaluate(source)

    assert_equal expected, actual,
                 "expected #{source.inspect} to evaluate to #{expected.inspect}"
  end

  # Asserts that +source+ fails with +error_class+ and an exact message.
  def assert_calculator_error(error_class, message, source)
    error = assert_raises(error_class, "expected #{source.inspect} to raise #{error_class}") do
      calculate(source)
    end

    assert_equal message, error.message, "for #{source.inspect}"
  end
end

# Base class for the suite; every test class inherits the helpers above.
class CalculatorTestCase < Minitest::Test
  include CalculatorTestHelpers
end
