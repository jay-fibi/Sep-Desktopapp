# frozen_string_literal: true

module Calculator
  # Command-line front end for the calculator.
  #
  #   Calculator::CLI.new.run([])                 # interactive shell
  #   Calculator::CLI.new.run(['1 + 2 * 3'])      # one-shot, prints 7
  #
  # The streams are injectable so the class can be tested without spawning a
  # process.
  class CLI
    PROMPT = 'calc> '
    RESULT_PREFIX = '=> '
    QUIT_COMMANDS = %w[quit exit q].freeze
    HELP_COMMANDS = %w[help h ?].freeze
    VARIABLE_COMMANDS = %w[vars variables].freeze
    CLEAR_COMMANDS = %w[clear reset].freeze

    def initialize(input: $stdin, output: $stdout, error: $stderr)
      @input = input
      @output = output
      @error = error
      @engine = Engine.new
    end

    # Runs the calculator and returns the process exit status: 0 on success
    # and 1 when the requested expression could not be evaluated.
    def run(arguments)
      case arguments.first
      when '-h', '--help' then print_help
      when '-v', '--version'
        @output.puts("calculator #{VERSION}")
        0
      when nil then run_interactive
      else run_once(arguments.join(' '))
      end
    rescue Interrupt
      @output.puts
      0
    end

    private

    def print_help
      @output.puts(help_text)
      0
    end

    def run_once(source)
      @output.puts(Format.number(@engine.evaluate(source)))
      0
    rescue Error => error
      report(error)
      1
    end

    def run_interactive
      @output.puts(banner)
      loop do
        @output.print(PROMPT)
        @output.flush
        line = @input.gets
        if line.nil?
          @output.puts
          break
        end

        line = line.strip
        next if line.empty?
        break if handle(line)
      end
      0
    end

    # Handles one line of interactive input and reports whether it asked to
    # quit. Commands are executed, anything else is evaluated as an
    # expression.
    def handle(line)
      command = line.downcase
      if QUIT_COMMANDS.include?(command)
        true
      elsif HELP_COMMANDS.include?(command)
        @output.puts(help_text)
        false
      elsif VARIABLE_COMMANDS.include?(command)
        print_variables
        false
      elsif CLEAR_COMMANDS.include?(command)
        clear_variables
        false
      else
        evaluate_line(line)
        false
      end
    end

    def evaluate_line(line)
      @output.puts("#{RESULT_PREFIX}#{Format.number(@engine.evaluate(line))}")
    rescue Error => error
      report(error)
    end

    def print_variables
      @engine.variables.sort.each do |name, value|
        @output.puts("#{name} = #{Format.number(value)}")
      end
    end

    def clear_variables
      @engine.reset
      @output.puts('variables cleared')
    end

    def report(error)
      @error.puts("Error: #{error.message}")
    end

    def banner
      "Calculator #{VERSION} - type an expression such as 2 * (3 + 4), " \
        "or 'help' for usage, 'quit' to leave."
    end

    def help_text
      [
        "Calculator #{VERSION}",
        '',
        'Enter an expression and press return, for example:',
        '  2 * (3 + 4)                   => 14',
        '  sqrt(2) ** 2                  => 2',
        '  radius = 3; pi * radius ** 2  => 28.2743338823',
        '',
        'Operators : + - * / % **, unary + and -, parentheses for grouping',
        'Constants : ' + Engine::CONSTANTS.keys.sort.join(', '),
        "Variables : name = expression (use 'vars' to list them, 'ans' holds the",
        '            previous result)',
        'Functions :',
        *Engine::FUNCTIONS.sort.map do |_name, function|
          Kernel.format('  %-18s %s', function.signature, function.description)
        end,
        '',
        'Commands  : help, vars, clear, quit',
        'Statements are separated by ";" and the value of the last one is shown.'
      ].join("\n")
    end
  end
end
