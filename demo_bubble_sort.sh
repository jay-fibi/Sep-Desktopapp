#!/usr/bin/env bash
# Demo: bubble sort an array of integers

bubble_sort() {
  local -a arr=("$@")
  local n=${#arr[@]}
  for ((i = 0; i < n - 1; i++)); do
    for ((j = 0; j < n - i - 1; j++)); do
      if ((arr[j] > arr[j + 1])); then
        local tmp=${arr[j]}
        arr[j]=${arr[j + 1]}
        arr[j + 1]=$tmp
      fi
    done
  done
  echo "${arr[@]}"
}

numbers=(5 2 9 1 5 6)
echo "Before: ${numbers[*]}"
echo "After:  $(bubble_sort "${numbers[@]}")"
