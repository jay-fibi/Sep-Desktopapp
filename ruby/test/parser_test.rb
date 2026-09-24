# frozen_string_literal: true

require_relative 'test_helper'

# Exercises Calculator::Parser, including the exact shape of the syntax tree
# and the errors it reports.
class ParserTest < CalculatorTestCase
  AST = Calculator::AST

  def parse(source)
    Calculator::Parser.new(Calculator::Lexer.new(source).tokens).parse
  end

  # Convenience constructors that keep the assertions readable.
  def number(value)
    AST::Number.new(value)
  end

  def binary(operator, left, right)
    AST::Binary.new(operator, left, right)
  end

  def test_multiplication_binds_tighter_than_addition
    assert_equal binary('+', number(1), binary('*', number(2), number(3))), parse('1 + 2 * 3').first
  end

  def test_operators_of_equal_precedence_are_left_associative
    assert_equal binary('-', binary('-', number(9), number(4)), number(3)), parse('9 - 4 - 3').first
  end

  def test_parentheses_override_precedence
    assert_equal binary('*', binary('+', number(2), number(3)), number(4)), parse('(2 + 3) * 4').first
  end

  def test_power_is_right_associative
    assert_equal binary('**', number(2), binary('**', number(3), number(2))), parse('2 ** 3 ** 2').first
  end

  def test_power_binds_tighter_than_unary_minus
    assert_equal AST::Unary.new('-', binary('**', number(2), number(2))), parse('-2 ** 2').first
  end

  def test_exponents_may_be_signed
    assert_equal binary('**', number(2), AST::Unary.new('-', number(1))), parse('2 ** -1').first
  end

  def test_variables_and_calls
    assert_equal AST::Variable.new('x'), parse('x').first
    assert_equal AST::Call.new('sqrt', [number(2)]), parse('sqrt(2)').first
    assert_equal AST::Call.new('max', [number(1), number(2), number(3)]), parse('max(1, 2, 3)').first
  end

  def test_empty_argument_list
    assert_equal AST::Call.new('now', []), parse('now()').first
  end

  def test_call_arguments_are_full_expressions
    assert_equal AST::Call.new('sqrt', [binary('+', number(1), number(2))]), parse('sqrt(1 + 2)').first
  end

  def test_assignment
    assert_equal AST::Assignment.new('x', number(1)), parse('x = 1').first
  end

  def test_assignment_is_right_associative
    assert_equal AST::Assignment.new('x', AST::Assignment.new('y', number(1))), parse('x = y = 1').first
  end

  def test_assignment_inside_a_call_or_parenthesis
    assert_equal AST::Call.new('sqrt', [AST::Assignment.new('x', number(4))]), parse('sqrt(x = 4)').first
    assert_equal binary('+', AST::Assignment.new('x', number(4)), AST::Variable.new('x')), parse('(x = 4) + x').first
  end

  def test_statement_separators
    assert_equal 3, parse('1; 2; 3').length
    assert_equal 3, parse('1; 2; 3;').length, 'a trailing separator is allowed'
  end

  def test_unexpected_end_of_expression
    assert_parser_error 'unexpected end of expression', '1 +'
    assert_parser_error 'unexpected end of expression', ''
  end

  def test_unclosed_parenthesis
    assert_parser_error "expected ')' but found end of expression", '(1 + 2'
  end

  def test_missing_comma_between_arguments
    assert_parser_error "expected ',' but found number 3", 'min(1 3)'
  end

  def test_stray_tokens_are_reported_with_their_column
    assert_parser_error 'unexpected number 3 at column 6', '1 + 2 3'
    assert_parser_error "unexpected ')' at column 0", ')'
    assert_parser_error "unexpected ',' at column 4", 'min(,1)'
    assert_parser_error "unexpected ';' at column 3", '1 +; 2'
  end

  private

  def assert_parser_error(message, source)
    assert_calculator_error Calculator::ParseError, message, source
  end
end
