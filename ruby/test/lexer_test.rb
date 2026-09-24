# frozen_string_literal: true

require_relative 'test_helper'

# Exercises Calculator::Lexer, which turns source text into tokens.
class LexerTest < CalculatorTestCase
  def tokens(source)
    Calculator::Lexer.new(source).tokens
  end

  def test_blank_input_produces_no_tokens
    assert_empty tokens('')
    assert_empty tokens("  \t\n ")
  end

  def test_whole_numbers_stay_integers
    token = tokens('42').first

    assert_equal :number, token.type
    assert_equal 42, token.value
    assert_instance_of Integer, token.value
  end

  def test_decimal_and_exponent_literals_become_floats
    ['3.14', '.5', '1.', '6.02e23', '1E-9'].each do |literal|
      token = tokens(literal).first

      assert_equal :number, token.type, literal
      assert_instance_of Float, token.value, literal
    end
  end

  def test_number_values
    assert_equal 3.14, tokens('3.14').first.value
    assert_equal 0.5, tokens('.5').first.value
    assert_equal 1.0, tokens('1.').first.value
    assert_equal 1e-9, tokens('1E-9').first.value
    assert_in_delta 6.02e23, tokens('6.02e23').first.value
  end

  def test_names_allow_digits_and_underscores
    token = tokens('sqrt_2').first

    assert_equal :name, token.type
    assert_equal 'sqrt_2', token.value
  end

  def test_multi_character_operators_are_not_split
    assert_equal [%w[operator **], %w[operator +]], tokens('** +').map { |token| [token.type.to_s, token.value] }
  end

  def test_symbols
    types = tokens('(1, 2); x = 3').map(&:type)

    assert_equal %i[lparen number comma number rparen separator name assign number], types
  end

  def test_columns_point_at_the_start_of_each_token
    assert_equal [0, 2, 4], tokens('1 + 2').map(&:column)
    assert_equal 5, tokens('  1 +2').last.column
  end

  def test_unknown_character_is_reported_with_its_column
    error = assert_raises(Calculator::ParseError) { tokens('1 @ 2') }

    assert_equal 'unexpected character "@" at column 2', error.message
  end

  def test_newlines_separate_tokens
    assert_equal [1, 2, 3], tokens("1\n2\t3").map(&:value)
  end
end
