# frozen_string_literal: true

module Calculator
  # Recursive-descent parser that turns tokens into AST nodes.
  #
  # Grammar, from lowest to highest precedence:
  #
  #   program        := statement (';' statement)* ';'?
  #   statement      := NAME '=' statement | additive
  #   additive       := multiplicative (('+' | '-') multiplicative)*
  #   multiplicative := unary (('*' | '/' | '%') unary)*
  #   unary          := ('+' | '-') unary | power
  #   power          := primary ('**' unary)?
  #   primary        := NUMBER | NAME | NAME '(' arguments ')' | '(' statement ')'
  #   arguments      := (statement (',' statement)*)?
  #
  # Notes on precedence choices:
  #
  # * +**+ binds tighter than unary minus, so <tt>-2 ** 2</tt> is -4.
  # * +**+ is right associative, so <tt>2 ** 3 ** 2</tt> is 512.
  # * The exponent may carry its own sign, so <tt>2 ** -1</tt> is 0.5.
  # * A function's arguments may be full expressions, including assignments.
  class Parser
    def initialize(tokens)
      @tokens = tokens
      @index = 0
    end

    # Returns the list of statements in the program. Every statement is a
    # complete expression, so an empty source raises ParseError.
    def parse
      statements = []
      loop do
        statements << parse_statement
        break unless accept(:separator)
        break if current.nil?
      end
      expect_end!

      statements
    end

    private

    def parse_statement
      if current&.type == :name && peek(1)&.type == :assign
        name = advance.value
        advance # the '='
        AST::Assignment.new(name, parse_statement)
      else
        parse_additive
      end
    end

    def parse_additive
      node = parse_multiplicative
      while operator?('+', '-')
        node = AST::Binary.new(advance.value, node, parse_multiplicative)
      end
      node
    end

    def parse_multiplicative
      node = parse_unary
      while operator?('*', '/', '%')
        node = AST::Binary.new(advance.value, node, parse_unary)
      end
      node
    end

    def parse_unary
      return parse_power unless operator?('+', '-')

      AST::Unary.new(advance.value, parse_unary)
    end

    def parse_power
      node = parse_primary
      node = AST::Binary.new(advance.value, node, parse_unary) if operator?('**')
      node
    end

    def parse_primary
      token = current
      raise ParseError, 'unexpected end of expression' if token.nil?

      case token.type
      when :number
        advance
        AST::Number.new(token.value)
      when :name
        advance
        parse_name(token)
      when :lparen
        advance
        node = parse_statement
        expect(:rparen)
        node
      else
        raise ParseError, "unexpected #{describe(token)} at column #{token.column}"
      end
    end

    # A bare name is a variable; a name followed by '(' is a function call.
    def parse_name(token)
      return AST::Variable.new(token.value) unless accept(:lparen)

      arguments = []
      unless accept(:rparen)
        loop do
          arguments << parse_statement
          break if accept(:rparen)

          expect(:comma)
        end
      end
      AST::Call.new(token.value, arguments)
    end

    def current
      @tokens[@index]
    end

    def peek(offset)
      @tokens[@index + offset]
    end

    def advance
      token = @tokens[@index]
      @index += 1 if token
      token
    end

    def operator?(*candidates)
      token = current
      token&.type == :operator && candidates.include?(token.value)
    end

    def accept(type)
      token = current
      return nil unless token&.type == type

      advance
    end

    def expect(type)
      accept(type) || raise(ParseError, "expected #{TYPE_NAMES.fetch(type, type.to_s)} but found #{describe(current)}")
    end

    def expect_end!
      return if current.nil?

      raise ParseError, "unexpected #{describe(current)} at column #{current.column}"
    end

    def describe(token)
      return 'end of expression' if token.nil?

      case token.type
      when :number then "number #{Format.number(token.value)}"
      when :name then "name '#{token.value}'"
      else "'#{token.value}'"
      end
    end

    TYPE_NAMES = {
      lparen: "'('",
      rparen: "')'",
      comma: "','",
      number: 'a number',
      name: 'a name',
      separator: "';'"
    }.freeze
  end
end
