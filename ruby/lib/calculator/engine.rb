# frozen_string_literal: true

module Calculator
  # Evaluates syntax trees against a mutable variable scope.
  #
  #   engine = Engine.new
  #   engine.evaluate('radius = 3')       # => 3
  #   engine.evaluate('pi * radius ** 2') # => 28.274333882308138
  #   engine.variables                    # => {"ans" => ..., "radius" => 3}
  #
  # +ans+ always holds the value of the previous evaluation, which makes it
  # easy to chain calculations in the interactive shell.
  class Engine
    # Values that are available without being assigned first.
    CONSTANTS = {
      'pi' => Math::PI,
      'e' => Math::E,
      'tau' => Math::PI * 2
    }.freeze

    # A named function usable in expressions. The arity a function accepts is
    # derived from the parameters of its implementation, so its signature, its
    # argument check and its error message cannot drift apart.
    class Function
      attr_reader :signature, :description, :implementation

      def initialize(signature, description, implementation)
        @signature = signature
        @description = description
        @implementation = implementation
        @minimum, @maximum = arity_range(implementation)
      end

      def call(arguments)
        @implementation.call(*arguments)
      end

      def accepts?(count)
        count >= @minimum && count <= @maximum
      end

      # Human readable argument count: "1", "1 to 2" or "1 or more".
      def arity_description
        return @minimum.to_s if @maximum == @minimum
        return "#{@minimum} or more" if @maximum.infinite?

        "#{@minimum} to #{@maximum}"
      end

      private

      def arity_range(implementation)
        parameters = implementation.parameters
        required = parameters.count { |kind, _name| kind == :req }
        optional = parameters.count { |kind, _name| kind == :opt }
        variadic = parameters.any? { |kind, _name| kind == :rest }
        maximum = variadic ? Float::INFINITY : required + optional

        [required, maximum]
      end
    end

    # Every supported function, keyed by the name used in expressions.
    FUNCTIONS = {
      'abs' => Function.new('abs(x)', 'absolute value', ->(x) { x.abs }),
      'acos' => Function.new('acos(x)', 'arc cosine, x in [-1, 1]', ->(x) { Math.acos(x) }),
      'asin' => Function.new('asin(x)', 'arc sine, x in [-1, 1]', ->(x) { Math.asin(x) }),
      'atan' => Function.new('atan(x)', 'arc tangent', ->(x) { Math.atan(x) }),
      'atan2' => Function.new('atan2(y, x)', 'angle of the point (x, y)', ->(y, x) { Math.atan2(y, x) }),
      'cbrt' => Function.new('cbrt(x)', 'cube root, also for negative x', ->(x) { Math.cbrt(x) }),
      'ceil' => Function.new('ceil(x)', 'smallest integer >= x', ->(x) { x.ceil }),
      'cos' => Function.new('cos(x)', 'cosine of x radians', ->(x) { Math.cos(x) }),
      'degrees' => Function.new('degrees(x)', 'radians to degrees', ->(x) { x * 180.0 / Math::PI }),
      'exp' => Function.new('exp(x)', 'e raised to the power x', ->(x) { Math.exp(x) }),
      'fact' => Function.new('fact(n)', 'factorial of a whole number n <= 1000', ->(n) { Engine.factorial(n) }),
      'floor' => Function.new('floor(x)', 'largest integer <= x', ->(x) { x.floor }),
      'hypot' => Function.new('hypot(x, y)', 'sqrt(x**2 + y**2)', ->(x, y) { Math.hypot(x, y) }),
      'log' => Function.new('log(x, base = e)', 'logarithm of x to the given base', ->(x, base = Math::E) { Math.log(x) / Math.log(base) }),
      'log10' => Function.new('log10(x)', 'base 10 logarithm', ->(x) { Math.log10(x) }),
      'log2' => Function.new('log2(x)', 'base 2 logarithm', ->(x) { Math.log2(x) }),
      'max' => Function.new('max(x, y, ...)', 'largest of its arguments', ->(first, *rest) { [first, *rest].max }),
      'min' => Function.new('min(x, y, ...)', 'smallest of its arguments', ->(first, *rest) { [first, *rest].min }),
      'pow' => Function.new('pow(x, y)', 'x raised to the power y', ->(x, y) { x**y }),
      'radians' => Function.new('radians(x)', 'degrees to radians', ->(x) { x * Math::PI / 180.0 }),
      'round' => Function.new('round(x, digits = 0)', 'round to the given number of decimals', ->(x, digits = 0) { x.round(digits) }),
      'sign' => Function.new('sign(x)', '-1, 0 or 1 depending on the sign of x', ->(x) { x.negative? ? -1 : (x.zero? ? 0 : 1) }),
      'sin' => Function.new('sin(x)', 'sine of x radians', ->(x) { Math.sin(x) }),
      'sqrt' => Function.new('sqrt(x)', 'square root, x >= 0', ->(x) { Math.sqrt(x) }),
      'tan' => Function.new('tan(x)', 'tangent of x radians', ->(x) { Math.tan(x) }),
      'truncate' => Function.new('truncate(x)', 'discard the fractional part', ->(x) { x.truncate })
    }.transform_values(&:freeze).freeze

    # The scratch variable that holds the previous result.
    ANSWER = 'ans'

    # Limit for Integer ** Integer, which Ruby computes exactly: without a cap
    # an expression such as 2 ** 10 ** 9 would try to build a number with
    # billions of digits. Float exponents simply overflow to Infinity, which
    # check_range! rejects.
    MAX_INTEGER_EXPONENT = 1_000_000

    # Factorial is a class method rather than a lambda so that its argument
    # checks stay readable; the FUNCTIONS entry above simply calls it.
    def self.factorial(number)
      number = number.to_i if integral_float?(number)
      raise EvaluationError, 'fact expects a whole number' unless number.is_a?(Integer)
      raise EvaluationError, 'fact expects a non-negative number' if number.negative?
      raise EvaluationError, 'fact is limited to n <= 1000' if number > 1000

      (1..number).reduce(1, :*)
    end

    # A Float that holds a whole number, e.g. 5.0 but not 5.5.
    def self.integral_float?(value)
      value.is_a?(Float) && value.finite? && value == value.truncate
    end

    attr_reader :variables

    # +variables+ seeds the scope. The hash is copied, so callers keep
    # ownership of what they pass in.
    def initialize(variables: {})
      @variables = { ANSWER => 0 }.merge(variables)
    end

    # Evaluates the statements in +source+ and returns the value of the last
    # one, which is also stored in +ans+.
    #
    # Raises ParseError for malformed input and EvaluationError for anything
    # that only fails once values are known.
    def evaluate(source)
      statements = Parser.new(Lexer.new(source).tokens).parse
      result = statements.reduce(0) { |_previous, statement| evaluate_node(statement) }

      @variables[ANSWER] = result
    end

    # Forgets every variable except +ans+, which is reset to 0.
    def reset
      @variables.clear
      @variables[ANSWER] = 0
      self
    end

    private

    def evaluate_node(node)
      case node
      when AST::Number then node.value
      when AST::Variable then lookup(node.name)
      when AST::Unary then apply_unary(node.operator, evaluate_node(node.operand))
      when AST::Binary then apply_binary(node.operator, evaluate_node(node.left), evaluate_node(node.right))
      when AST::Call then apply_function(node)
      when AST::Assignment then assign(node)
      else
        raise EvaluationError, "cannot evaluate #{node.class}"
      end
    end

    def assign(node)
      value = evaluate_node(node.expression)
      @variables[node.name] = value
    end

    def apply_unary(operator, operand)
      check_range!(operator == '-' ? -operand : operand)
    end

    def apply_binary(operator, left, right)
      result =
        case operator
        when '+' then left + right
        when '-' then left - right
        when '*' then left * right
        when '/' then divide(left, right)
        when '%' then remainder(left, right)
        when '**' then power(left, right)
        else
          raise EvaluationError, "unsupported operator '#{operator}'"
        end

      check_range!(result)
    end

    # Division always produces a Float, so 10 / 4 is 2.5 rather than 2.
    def divide(left, right)
      raise EvaluationError, 'Cannot divide by zero' if right.zero?

      left.fdiv(right)
    end

    def remainder(left, right)
      raise EvaluationError, 'Cannot take a remainder modulo zero' if right.zero?

      left % right
    end

    # A negative base with a fractional exponent has no real result; Ruby
    # silently returns a Complex number, so turn that into a clear error.
    def power(base, exponent)
      check_exponent!(base, exponent)
      result = base**exponent
      raise EvaluationError, 'result is not a real number' if result.is_a?(Complex)

      result
    rescue ::ZeroDivisionError
      # Ruby raises this for 0 ** -1, where the result would be infinite.
      raise EvaluationError, 'Cannot divide by zero'
    end

    # Integer ** Integer is exact, which is nice for 2 ** 100 but would try to
    # allocate gigabytes for 2 ** 10 ** 9. Anything bigger overflows to
    # Infinity once a Float is involved, and is rejected by check_range!.
    def check_exponent!(base, exponent)
      return unless base.is_a?(Integer) && exponent.is_a?(Integer)
      return if exponent <= MAX_INTEGER_EXPONENT

      raise EvaluationError,
            "exponent is limited to #{MAX_INTEGER_EXPONENT} for whole-number bases"
    end

    def apply_function(node)
      function = FUNCTIONS[node.name]
      raise EvaluationError, "unknown function '#{node.name}'" if function.nil?

      unless function.accepts?(node.arguments.length)
        raise EvaluationError,
              "#{node.name} expects #{function.arity_description} argument(s) " \
              "but got #{node.arguments.length}"
      end

      arguments = node.arguments.map { |argument| evaluate_node(argument) }

      begin
        check_range!(function.call(arguments))
      rescue Math::DomainError, RangeError
        raise EvaluationError, "#{node.name} is not defined for these arguments"
      end
    end

    def lookup(name)
      return @variables[name] if @variables.key?(name)
      return CONSTANTS[name] if CONSTANTS.key?(name)

      raise EvaluationError, "unknown variable '#{name}'"
    end

    # Rejects results Ruby represents as Complex or as Float::INFINITY/NaN,
    # so that every value leaving the engine is a plain, usable number.
    # Rational results (2 ** -1 is 1/2) are returned as Floats so that the
    # arithmetic domain stays Integer-or-Float.
    def check_range!(value)
      value = value.to_f if value.is_a?(Rational)
      raise EvaluationError, 'result is not a real number' if value.is_a?(Complex)

      unless value.finite?
        raise EvaluationError, 'result is outside the supported numeric range'
      end

      value
    end
  end
end
