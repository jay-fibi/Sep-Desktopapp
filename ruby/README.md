# Ruby Calculator

A small command-line calculator written in Ruby, with a reusable expression
library behind it. It evaluates general arithmetic expressions, remembers
variables, and needs no gems at runtime — only the standard library.

```
$ ./bin/calculator
Calculator 1.0.0 - type an expression such as 2 * (3 + 4), or 'help' for usage, 'quit' to leave.
calc> 2 * (3 + 4)
=> 14
calc> radius = 3; pi * radius ** 2
=> 28.2743338823
calc> sqrt(2) ** 2
=> 2
calc> fact(20) / fact(18)
=> 380
calc> quit
```

One-shot mode evaluates the arguments and prints the result, which makes it
usable in scripts:

```
$ ./bin/calculator '10 / 4'
2.5
$ ./bin/calculator 1 + 2
3
$ ./bin/calculator '1 / 0'; echo "exit status: $?"
Error: Cannot divide by zero
exit status: 1
```

## Requirements

Ruby 3.1 or newer. There are no runtime dependencies; `rake` and `minitest`
are only needed to run the test suite (both ship with Ruby).

## Expression syntax

| Feature | Examples |
| --- | --- |
| Numbers | `42`, `3.14`, `.5`, `6.02e23`, `1e-9` |
| Operators | `+`, `-`, `*`, `/`, `%`, `**` (also unary `+` and `-`) |
| Grouping | `(2 + 3) * 4` |
| Variables | `radius = 3`, then `radius ** 2` |
| Statements | `x = 2; y = x ** 10; y` |
| Constants | `pi`, `e`, `tau` |
| Functions | `sqrt(2)`, `round(2.567, 2)`, `min(1, 2, 3)` |

Notes on semantics:

* `**` binds tighter than unary minus, so `-2 ** 2` is `-4`, and it is right
  associative, so `2 ** 3 ** 2` is `512`.
* `/` always produces a Float, so `10 / 4` is `2.5` rather than Ruby's
  integer division `2`.
* `%` follows Ruby semantics: the result takes the sign of the divisor, so
  `-7 % 3` is `2`.
* Whole numbers stay exact, so `2 ** 100` and `fact(30)` print every digit.
* Names are case sensitive; assigning a constant shadows it for the session.
* `ans` holds the previous result and starts at `0`.

### Functions

| Signature | Description |
| --- | --- |
| `abs(x)` | absolute value |
| `sqrt(x)`, `cbrt(x)` | square root (x >= 0), cube root |
| `pow(x, y)` | x raised to the power y |
| `exp(x)`, `log(x, base = e)`, `log2(x)`, `log10(x)` | exponentials and logarithms |
| `floor(x)`, `ceil(x)`, `round(x, digits = 0)`, `truncate(x)` | rounding |
| `min(x, y, ...)`, `max(x, y, ...)` | smallest or largest argument |
| `sin(x)`, `cos(x)`, `tan(x)`, `asin(x)`, `acos(x)`, `atan(x)`, `atan2(y, x)` | trigonometry (radians) |
| `hypot(x, y)` | `sqrt(x**2 + y**2)` |
| `degrees(x)`, `radians(x)` | convert between radians and degrees |
| `sign(x)` | `-1`, `0` or `1` |
| `fact(n)` | factorial of a whole number `n <= 1000` |

Run `help` in the interactive shell for the same list, generated from the
function table itself.

### Interactive commands

| Command | Effect |
| --- | --- |
| `help`, `h`, `?` | show usage |
| `vars`, `variables` | list the current variables |
| `clear`, `reset` | forget every variable |
| `quit`, `exit`, `q` | leave the calculator |

## Using the library

```ruby
require_relative 'lib/calculator'

Calculator.evaluate('2 * (3 + 4)')                # => 14
Calculator.evaluate('x = 2; x ** 10')             # => 1024
Calculator.evaluate('radius ** 2', variables: { 'radius' => 3 }) # => 9

engine = Calculator::Engine.new
engine.evaluate('radius = 3')     # => 3
engine.evaluate('pi * radius ** 2') # => 28.274333882308138
engine.variables                  # => {"ans" => 28.27..., "radius" => 3}
engine.reset                      # forget everything

Calculator::Format.number(engine.variables['ans']) # => "28.2743338823"
```

The parser is exposed as well, so expressions can be inspected instead of
evaluated:

```ruby
tokens = Calculator::Lexer.new('1 + 2 * 3').tokens
Calculator::Parser.new(tokens).parse
# => [#<struct Calculator::AST::Binary operator="+", ...>]
```

Every error derives from `Calculator::Error`, which is either a
`ParseError` (the text is malformed) or an `EvaluationError` (the arithmetic
cannot be carried out):

```ruby
begin
  Calculator.evaluate('1 / 0')
rescue Calculator::Error => error
  warn error.message # => "Cannot divide by zero"
end
```

Errors reported by the engine include division by zero, an unknown variable
or function, a wrong argument count (for example `sqrt(1, 2)`), a result
outside the supported numeric range (`1e308 * 10`), and a non-real result
such as `(-8) ** (1 / 3)`.

## Running the tests

```
$ rake test
98 runs, 278 assertions, 0 failures, 0 errors, 0 skips
```

Without rake, minitest can be invoked directly:

```
$ ruby -Ilib -Itest test/calculator_test.rb
```

Useful extras:

```
$ rake console                     # IRB with Calculator loaded
$ rake 'evaluate[2 * (3 + 4)]'     # 14
```

## Project layout

```
bin/calculator            executable entry point
lib/calculator.rb         namespace, errors and Calculator.evaluate
lib/calculator/ast.rb     syntax tree node definitions
lib/calculator/lexer.rb   source text to tokens
lib/calculator/parser.rb  tokens to syntax tree (recursive descent)
lib/calculator/engine.rb  syntax tree to numbers, plus the function table
lib/calculator/format.rb  number formatting for display
lib/calculator/cli.rb     interactive shell and one-shot mode
test/                     minitest suite (lexer, parser, engine, CLI, format)
```

The pipeline is deliberately split into stages: the lexer never interprets,
the parser never computes, and the engine never sees source text. That keeps
each piece small and lets the tests target one stage at a time.

## Implementation notes

* Numbers are Ruby `Integer`s when written without a decimal point or
  exponent, and `Float`s otherwise. Division and roots produce `Float`s, so
  results are always either exact integers or doubles.
* Results are printed with at most 12 significant digits, which hides
  floating point noise (`0.1 + 0.2` prints as `0.3`) without dropping useful
  precision (`1 / 3` prints as `0.333333333333`).
* `2 ** 10 ** 9` is rejected instead of trying to build a billion-digit
  number, and `fact(n)` is limited to `n <= 1000` for the same reason.
* The interactive shell and the one-shot mode share one `Engine`, so
  variables survive across lines but never across processes.
