/*
 * test.js — headless sanity checks for the visualizer's core logic.
 * Run with:  node test.js
 *
 * Verifies, for every algorithm:
 *   1. The generator fully sorts arrays of many shapes (random, sorted,
 *      reversed, duplicates-heavy, tiny).
 *   2. Every emitted event references valid array indices.
 *   3. Every emitted `lines` entry is a valid 1-based line number into
 *      the matching JAVA_CODE snippet (so code highlighting never misses).
 *   4. 'set' events carry the value actually written to the array.
 *   5. Comparison/swap counters are plausible (> 0 for non-trivial input).
 */
'use strict';

const { JAVA_CODE, GENERATORS, ORDER } = require('./algorithms.js');

let failures = 0;

function check(cond, msg) {
  if (!cond) {
    failures++;
    console.error('  ✗ FAIL:', msg);
  }
}

function isSorted(a) {
  for (let i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
  return true;
}

function makeCases() {
  const rnd = (n, lo, hi) =>
    Array.from({ length: n }, () => lo + Math.floor(Math.random() * (hi - lo + 1)));
  const cases = [
    ['empty', []],
    ['single', [42]],
    ['two sorted', [1, 2]],
    ['two reversed', [2, 1]],
    ['all equal', [7, 7, 7, 7, 7]],
    ['already sorted', Array.from({ length: 40 }, (_, i) => i + 1)],
    ['reversed', Array.from({ length: 40 }, (_, i) => 40 - i)],
    ['few unique', rnd(60, 1, 4)],
  ];
  for (let s = 0; s < 25; s++) cases.push([`random#${s}`, rnd(5 + Math.floor(Math.random() * 120), 5, 100)]);
  return cases;
}

for (const algo of ORDER) {
  const gen = GENERATORS[algo];
  const lineCount = JAVA_CODE[algo].split('\n').length;
  console.log(`Testing ${algo} (snippet has ${lineCount} lines)…`);

  for (const [label, input] of makeCases()) {
    const arr = [...input];
    const n = arr.length;
    let comparisons = 0;
    let writes = 0;

    for (const ev of gen(arr)) {
      // Valid event shape
      check(typeof ev.type === 'string', `${algo}/${label}: event has a type`);
      // Valid indices
      for (const key of ['i', 'j']) {
        if (ev[key] !== undefined) {
          check(Number.isInteger(ev[key]) && ev[key] >= 0 && ev[key] < n,
            `${algo}/${label}: ${ev.type} has valid ${key} (got ${ev[key]}, n=${n})`);
        }
      }
      // Valid line numbers
      check(Array.isArray(ev.lines) && ev.lines.length > 0,
        `${algo}/${label}: ${ev.type} carries highlight lines`);
      if (Array.isArray(ev.lines)) {
        for (const ln of ev.lines) {
          check(Number.isInteger(ln) && ln >= 1 && ln <= lineCount,
            `${algo}/${label}: line ${ln} within 1..${lineCount}`);
        }
      }
      if (ev.type === 'compare') comparisons++;
      if (ev.type === 'swap' || ev.type === 'set') {
        writes++;
        if (ev.type === 'set') {
          check(ev.value === arr[ev.i],
            `${algo}/${label}: set value matches array after write`);
        }
      }
    }

    check(isSorted(arr), `${algo}/${label}: array is sorted after run (got [${arr.slice(0, 8).join(',')}…])`);
    if (n > 4) {
      check(comparisons > 0, `${algo}/${label}: performed comparisons`);
      // Note: bubble & selection legitimately do 0 writes when the input is
      // already sorted (early-exit / no out-of-place minimum), so only assert
      // writes on inputs that actually need reordering.
      if (!isSorted(input)) {
        check(writes > 0, `${algo}/${label}: performed writes/swaps`);
      }
    }
    // Original input untouched check is implicit — we passed a copy.
  }
}

if (failures === 0) {
  console.log('\n✓ All checks passed for', ORDER.length, 'algorithms.');
} else {
  console.error(`\n✗ ${failures} check(s) failed.`);
  process.exit(1);
}
