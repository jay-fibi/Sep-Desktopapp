# frozen_string_literal: true

# Star pattern program: prints a few classic star patterns.

def right_triangle(rows)
  (1..rows).each { |i| puts "*" * i }
end

def inverted_triangle(rows)
  rows.downto(1) { |i| puts "*" * i }
end

def pyramid(rows)
  (1..rows).each do |i|
    puts " " * (rows - i) + "*" * (2 * i - 1)
  end
end

def diamond(rows)
  pyramid(rows)
  (rows - 1).downto(1) do |i|
    puts " " * (rows - i) + "*" * (2 * i - 1)
  end
end

def hollow_square(size)
  size.times do |i|
    if i.zero? || i == size - 1
      puts "*" * size
    else
      puts "*" + " " * (size - 2) + "*"
    end
  end
end

def main(rows = 5)
  puts "Right triangle:"
  right_triangle(rows)

  puts "\nInverted triangle:"
  inverted_triangle(rows)

  puts "\nPyramid:"
  pyramid(rows)

  puts "\nDiamond:"
  diamond(rows)

  puts "\nHollow square:"
  hollow_square(rows)
end

if __FILE__ == $PROGRAM_NAME
  rows = ARGV[0].to_i
  rows = 5 if rows <= 0
  main(rows)
end
