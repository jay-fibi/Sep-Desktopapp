# frozen_string_literal: true

module Calculator
  # Syntax tree nodes produced by Parser and consumed by Engine.
  #
  # Every node is a plain Struct, which makes the tree cheap to build and easy
  # to pattern-match against:
  #
  #   case node
  #   when AST::Number then node.value
  #   end
  #
  # Instances are created by the parser only and treated as read-only.
  module AST
    # A numeric literal: an Integer for whole numbers written without a
    # decimal point or exponent (so +2 ** 100+ stays exact), otherwise a
    # Float (+2.5+, +1e-3+).
    Number = Struct.new(:value)

    # A reference to a variable or constant, such as +x+ or +pi+.
    Variable = Struct.new(:name)

    # A prefix operator applied to a single operand, e.g. <tt>-x</tt>.
    Unary = Struct.new(:operator, :operand)

    # An infix operator applied to two operands, e.g. <tt>1 + 2</tt>.
    Binary = Struct.new(:operator, :left, :right)

    # A function call such as <tt>sqrt(2)</tt> or <tt>min(1, 2, 3)</tt>.
    Call = Struct.new(:name, :arguments)

    # An assignment such as <tt>radius = 3</tt>; evaluates to the assigned
    # value so that chained assignments work.
    Assignment = Struct.new(:name, :expression)
  end
end
