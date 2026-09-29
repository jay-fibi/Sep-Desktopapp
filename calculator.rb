#!/usr/bin/env ruby
# frozen_string_literal: true

# A simple command-line calculator.
#
# Usage (interactive):
#   ruby calculator.rb
#
# Usage (single expression):
#   ruby calculator.rb "2 + 3 * 4"

module Calculator
  # Evaluate a basic arithmetic expression like "2 + 3 * 4".
  # Supports +, -, *, /, %, parentheses, unary minus, integers and decimals.
  def self.evaluate(expression)
    tokens = tokenize(expression)
    raise ArgumentError, "empty expression" if tokens.empty?

    result = parse_expression(tokens)
    raise ArgumentError, "unexpected input near #{tokens.first.inspect}" unless tokens.empty?

    result
  end

  # Break an expression into numbers, operators and parentheses, dropping
  # any whitespace. Returns an array of String tokens.
  def self.tokenize(expression)
    expression.scan(/\d+(?:\.\d+)?|[+\-*\/\%()]/)
  end

  # Recursive-descent parser supporting +, -, *, /, % and parentheses.
  def self.parse_expression(tokens)
    left = parse_term(tokens)
    while %w[+ -].include?(tokens[0])
      op = tokens.shift
      right = parse_term(tokens)
      left = apply(op, left, right)
    end
    left
  end

  def self.parse_term(tokens)
    left = parse_factor(tokens)
    while %w[* / %].include?(tokens[0])
      op = tokens.shift
      right = parse_factor(tokens)
      left = apply(op, left, right)
    end
    left
  end

  def self.parse_factor(tokens)
    token = tokens.shift
    raise ArgumentError, "unexpected end of expression" if token.nil?

    if token == "("
      value = parse_expression(tokens)
      closing = tokens.shift
      raise ArgumentError, "missing closing parenthesis" unless closing == ")"
      return value
    end

    if token == "-"
      # Unary minus
      return -parse_factor(tokens)
    end

    begin
      Float(token)
    rescue ArgumentError
      raise ArgumentError, "invalid number: #{token.inspect}"
    end
  end

  def self.apply(op, a, b)
    case op
    when "+" then a + b
    when "-" then a - b
    when "*" then a * b
    when "/" then raise(ZeroDivisionError, "division by zero") if b.zero?
      a.to_f / b
    when "%" then raise(ZeroDivisionError, "modulo by zero") if b.zero?
      a % b
    else
      raise ArgumentError, "unknown operator: #{op}"
    end
  end
end

def interactive_mode
  puts "Simple Ruby Calculator"
  puts "Enter an expression (e.g. 2 + 3 * 4). Type 'exit' to quit."
  loop do
    print "> "
    input = $stdin.gets
    break if input.nil?

    input = input.strip
    next if input.empty?
    break if %w[exit quit q].include?(input.downcase)

    begin
      result = Calculator.evaluate(input)
      # Print integers without a trailing .0
      result = result.to_i if result.is_a?(Float) && result.frac.zero?
      puts "= #{result}"
    rescue StandardError => e
      puts "Error: #{e.message}"
    end
  end
  puts "Goodbye!"
end

if __FILE__ == $PROGRAM_NAME
  if ARGV.empty?
    interactive_mode
  else
    expression = ARGV.join(" ")
    begin
      result = Calculator.evaluate(expression)
      result = result.to_i if result.is_a?(Float) && result.frac.zero?
      puts "#{expression} = #{result}"
    rescue StandardError => e
      warn "Error: #{e.message}"
      exit 1
    end
  end
end
