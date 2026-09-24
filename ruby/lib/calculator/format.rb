# frozen_string_literal: true

module Calculator
  # Renders numeric results as short, human-friendly strings.
  #
  # The goal is to hide floating point noise (0.1 + 0.2 prints as 0.3) without
  # losing meaningful digits (1 / 3 prints as 0.333333333333).
  module Format
    # Number of significant digits kept for non-integer floats.
    PRECISION = 12

    # Largest magnitude rendered as a plain integer; everything bigger has
    # too many digits to read and is printed in scientific notation instead.
    MAX_PLAIN_INTEGER = 10**16

    # Formats +value+, which may be an Integer or a Float.
    #
    #   Format.number(10.0)                 # => "10"
    #   Format.number(0.1 + 0.2)            # => "0.3"
    #   Format.number(1.0 / 3)              # => "0.333333333333"
    #   Format.number(10**30)               # => "1000000000000000000000000000000"
    #   Format.number(1e20)                 # => "1e+20"
    #   Format.number(Float::NAN)           # => "NaN"
    #
    # The engine never returns an Infinity or a NaN, but the formatter is a
    # public API, so non-finite values fall back to Ruby's own spelling
    # ("Infinity", "-Infinity", "NaN") instead of raising.
    def self.number(value)
      return value.to_s if value.is_a?(Integer)

      float = value.to_f
      return float.to_s unless float.finite?

      if float == float.truncate && float.abs < MAX_PLAIN_INTEGER
        float.truncate.to_s
      else
        Kernel.format("%.#{PRECISION}g", float)
      end
    end
  end
end
