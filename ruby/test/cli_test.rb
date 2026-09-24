# frozen_string_literal: true

require 'open3'
require 'stringio'
require_relative 'test_helper'

# Exercises Calculator::CLI in one-shot and interactive modes, plus the real
# executable as a separate process.
class CLITest < CalculatorTestCase
  def run_cli(arguments, input: '')
    output = StringIO.new
    error = StringIO.new
    status = Calculator::CLI.new(input: StringIO.new(input), output: output, error: error).run(arguments)

    [status, output.string, error.string]
  end

  def test_one_shot_evaluation
    status, output, error = run_cli(['2 * (3 + 4)'])

    assert_equal 0, status
    assert_equal "14\n", output
    assert_empty error
  end

  def test_arguments_are_joined_into_a_single_expression
    status, output, = run_cli(['1', '+', '2'])

    assert_equal 0, status
    assert_equal "3\n", output
  end

  def test_one_shot_failure_prints_to_stderr_and_exits_nonzero
    status, output, error = run_cli(['1 / 0'])

    assert_equal 1, status
    assert_empty output
    assert_equal "Error: Cannot divide by zero\n", error
  end

  def test_syntax_errors_are_reported_too
    status, _output, error = run_cli(['1 +'])

    assert_equal 1, status
    assert_equal "Error: unexpected end of expression\n", error
  end

  def test_help_and_version
    status, output, = run_cli(['-h'])

    assert_equal 0, status
    assert_includes output, 'Operators'
    assert_includes output, 'sqrt(x)'

    status, output, = run_cli(['-v'])

    assert_equal 0, status
    assert_equal "calculator #{Calculator::VERSION}\n", output
  end

  def test_interactive_session
    input = "1 + 1\nradius = 3\nradius ** 2\nvars\nquit\n"
    status, output, error = run_cli([], input: input)

    assert_equal 0, status
    assert_empty error
    assert_includes output, 'Calculator'
    assert_includes output, "calc> => 2\n"
    assert_includes output, "calc> => 9\n"
    assert_includes output, "ans = 9\nradius = 3\n"
  end

  def test_interactive_errors_do_not_end_the_session
    input = "oops\n1 + 1\nquit\n"
    _status, output, error = run_cli([], input: input)

    assert_equal "Error: unknown variable 'oops'\n", error
    assert_includes output, '=> 2'
  end

  def test_interactive_helps_and_clears
    input = "help\nx = 1\nclear\nquit\n"
    _status, output, error = run_cli([], input: input)

    assert_empty error
    assert_includes output, 'Commands  : help, vars, clear, quit'
    assert_includes output, "variables cleared\n"
  end

  def test_interactive_exits_on_end_of_input
    status, output, = run_cli([], input: "1 + 1\n")

    assert_equal 0, status
    assert_includes output, '=> 2'
  end

  def test_blank_lines_are_ignored
    status, output, = run_cli([], input: "\n\n  \nquit\n")

    assert_equal 0, status
    refute_includes output, 'Error'
  end

  def test_all_quit_spellings
    %w[quit exit q QUIT].each do |word|
      status, output, = run_cli([], input: "1 + 1\n#{word}\n1 + 1\n")

      assert_equal 0, status, word
      assert_equal 1, output.scan('=> 2').length, "#{word} should stop the session"
    end
  end

  # The executable is the documented entry point, so exercise it directly.
  def test_executable_end_to_end
    executable = File.expand_path('../bin/calculator', __dir__)
    output, error, status = Open3.capture3(RbConfig.ruby, executable, '6 * 7')

    assert_equal 0, status.exitstatus
    assert_equal "42\n", output
    assert_empty error
  end

  def test_executable_failure_exit_status
    executable = File.expand_path('../bin/calculator', __dir__)
    output, error, status = Open3.capture3(RbConfig.ruby, executable, '1 / 0')

    assert_equal 1, status.exitstatus
    assert_empty output
    assert_equal "Error: Cannot divide by zero\n", error
  end

  def test_executable_pipes_stdin
    executable = File.expand_path('../bin/calculator', __dir__)
    output, _error, status = Open3.capture3(RbConfig.ruby, executable, stdin_data: "2 ** 10\nquit\n")

    assert_equal 0, status.exitstatus
    assert_includes output, '=> 1024'
  end
end
