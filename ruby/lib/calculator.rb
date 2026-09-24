# frozen_string_literal: true

# A small, dependency-free calculator library.
#
#   Calculator.evaluate('2 * (3 + 4)')  # => 14
#   Calculator.evaluate('x = 2; x ** 10') # => 1024
#
# The library is split into a lexer, a recursive-descent parser and an
# evaluator, so expressions can be inspected as well as evaluated:
#
#   tokens = Calculator::Lexer.new('1 + 2').tokens
#   ast    = Calculator::Parser.new(tokens).parse
#
# See README.md for the full expression syntax.
module Calculator
  VERSION = '1.0.0'

  # Base class of every error raised by this library, so callers can rescue
  # a single constant.
  class Error < StandardError; end

  # Raised when an expression cannot be turned into syntax tree nodes.
  class ParseError < Error; end

  # Raised when a well-formed expression cannot be evaluated: unknown names,
  # division by zero, results outside the supported numeric range, and so on.
  class EvaluationError < Error; end

  # Evaluates +source+ and returns the value of its last statement.
  #
  # +variables+ seeds the scope with initial values and is left untouched;
  # the built-in +ans+ variable always starts at 0.
  def self.evaluate(source, variables: {})
    Engine.new(variables: variables).evaluate(source)
  end
end

require_relative 'calculator/ast'
require_relative 'calculator/format'
require_relative 'calculator/lexer'
require_relative 'calculator/parser'
require_relative 'calculator/engine'
require_relative 'calculator/cli'
