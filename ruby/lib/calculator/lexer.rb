# frozen_string_literal: true

require 'strscan'

module Calculator
  # Splits a source string into a flat list of Token structs.
  #
  #   Lexer.new('1 + 2').tokens
  #   # => [#<Token :number 1 0>, #<Token :operator "+" 2>, #<Token :number 2 4>]
  #
  # Raises ParseError for characters that cannot start a token.
  class Lexer
    # A single lexical unit. +column+ is a zero-based index into the source
    # and is only used to build readable error messages.
    Token = Struct.new(:type, :value, :column)

    # Number literals: 42, 3.14, .5, 1., 6.02e23, 1E-9.
    NUMBER = /(?:\d+\.\d*|\.\d+|\d+)(?:[eE][+-]?\d+)?/
    # Identifiers: names of variables, constants and functions.
    NAME = /[A-Za-z_][A-Za-z0-9_]*/
    WHITESPACE = /[ \t\r\n]+/
    # Multi-character operators first so that +**+ wins over +*+.
    OPERATORS = %w[** + - * / %].sort_by { |operator| -operator.length }.freeze
    OPERATOR_PATTERN = Regexp.union(OPERATORS)
    SYMBOLS = {
      '(' => :lparen,
      ')' => :rparen,
      ',' => :comma,
      '=' => :assign,
      ';' => :separator
    }.freeze
    SYMBOL_PATTERN = Regexp.union(SYMBOLS.keys)
    # Largest value a Float can hold; literals above it are reported instead
    # of silently turning into Infinity.
    MAX_FLOAT = Float::MAX.to_r

    def initialize(source)
      @source = source
      @scanner = StringScanner.new(source)
    end

    # Returns the token list. The list is empty for a blank source string.
    def tokens
      result = []
      until @scanner.eos?
        @scanner.skip(WHITESPACE)
        break if @scanner.eos?

        result << next_token
      end
      result
    end

    private

    def next_token
      column = @scanner.pos

      if (number = @scanner.scan(NUMBER))
        Token.new(:number, number_value(number), column)
      elsif (name = @scanner.scan(NAME))
        Token.new(:name, name, column)
      elsif (operator = @scanner.scan(OPERATOR_PATTERN))
        Token.new(:operator, operator, column)
      elsif (symbol = @scanner.scan(SYMBOL_PATTERN))
        Token.new(SYMBOLS[symbol], symbol, column)
      else
        raise ParseError,
              "unexpected character #{@scanner.peek(1).inspect} at column #{column}"
      end
    end

    # Whole numbers stay Integers so that exact arithmetic (2 ** 100) keeps
    # working; anything with a decimal point or exponent becomes a Float.
    # Literals that overflow a Float (1e400) are rejected here rather than
    # turning into Infinity halfway through a calculation.
    def number_value(text)
      return Integer(text, 10) unless text.match?(/[.eE]/)

      float_value(text)
    end

    # Rational parses a decimal literal exactly, which makes the range check
    # reliable, and it avoids the "Float ... out of range" warning that
    # String#to_f writes to stderr for values above Float::MAX.
    def float_value(text)
      if Rational(text) > MAX_FLOAT
        raise ParseError, "number #{text} is outside the supported numeric range"
      end

      text.to_f
    end
  end
end
