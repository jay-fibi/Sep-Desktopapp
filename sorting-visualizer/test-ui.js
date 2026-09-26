/*
 * test-ui.js — headless UI integration test (requires jsdom).
 *
 * Setup:  npm install jsdom        # anywhere; set NODE_PATH if global/elsewhere
 * Run:    node test-ui.js          # or: NODE_PATH=/path/to/node_modules node test-ui.js
 *
 * Loads index.html + algorithms.js + app.js into jsdom and exercises the UI:
 * tabs, start/pause/step/reset, sliders, pattern select — for every algorithm.
 */
'use strict';

const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const DIR = __dirname;
const html = fs.readFileSync(path.join(DIR, 'index.html'), 'utf8');
const algorithmsJs = fs.readFileSync(path.join(DIR, 'algorithms.js'), 'utf8');
const appJs = fs.readFileSync(path.join(DIR, 'app.js'), 'utf8');

let failures = 0;
function check(cond, msg) {
  if (!cond) {
    failures++;
    console.error('  ✗ FAIL:', msg);
  }
}

const dom = new JSDOM(html, { runScripts: 'outside-only', pretendToBeVisual: true });
const { window } = dom;

// Execute the two scripts in page order
window.eval(algorithmsJs);
window.eval(appJs);

const $ = (id) => window.document.getElementById(id);

// --- Initial render ---
const tabs = [...window.document.querySelectorAll('.algo-tab')];
check(tabs.length === 6, `6 algorithm tabs rendered (got ${tabs.length})`);
check(tabs[0].textContent === 'Bubble Sort', 'first tab is Bubble Sort');
check($('barContainer').children.length === 60, 'bars rendered for default size 60');
check($('codeContent').querySelectorAll('.code-line').length === 16, 'bubble code has 16 numbered lines');
check($('complexityCard').innerHTML.includes('O(n²)'), 'complexity card shows O(n²)');
check($('statStatus').textContent === 'Ready', 'status starts Ready');

function drainByStepping(maxSteps = 5_000_000) {
  let n = 0;
  while ($('statStatus').textContent !== '✓ Sorted!' && n < maxSteps) {
    $('stepBtn').click();
    n++;
  }
  return n;
}

function barHeights() {
  return [...$('barContainer').children].map((b) => parseFloat(b.style.height));
}

function isSorted(arr) {
  return arr.every((v, i) => i === 0 || arr[i - 1] <= v);
}

// --- Full run for each algorithm: step to completion, verify sorted + stats ---
for (const tab of tabs) {
  tab.click();
  const algo = tab.dataset.algo;
  const before = barHeights();
  check(before.length === 60, `${algo}: 60 bars after tab switch`);
  check($('codeTitle').textContent === tab.textContent, `${algo}: code panel title updated`);
  check(window.document.querySelector('.algo-tab.active') === tab, `${algo}: tab marked active`);

  const steps = drainByStepping();
  check($('statStatus').textContent === '✓ Sorted!', `${algo}: reaches Sorted status (${steps} steps)`);
  check(isSorted(barHeights()), `${algo}: bars are ascending at the end`);
  const comps = Number($('statComparisons').textContent.replace(/,/g, ''));
  check(comps > 0, `${algo}: comparisons counted (${comps})`);
  const swaps = Number($('statSwaps').textContent.replace(/,/g, ''));
  check(swaps > 0, `${algo}: swaps/writes counted (${swaps})`);
  check(steps < 5_000_000, `${algo}: terminates within step budget`);

  $('resetBtn').click();
  const restored = barHeights();
  check(JSON.stringify(restored) === JSON.stringify(before), `${algo}: reset restores original array`);
  check($('statStatus').textContent === 'Ready', `${algo}: status back to Ready after reset`);
  check($('statComparisons').textContent === '0', `${algo}: stats cleared after reset`);
}

// --- Start / pause / resume via the primary button (async runner) ---
(async () => {
  tabs[0].click();
  $('startBtn').click();
  check($('startBtn').textContent === '⏸ Pause', 'start button becomes Pause while running');
  check($('statStatus').textContent === 'Sorting…', 'status shows Sorting…');
  check($('sizeSlider').disabled === true, 'size slider disabled while running');

  await new Promise((r) => setTimeout(r, 150));
  $('startBtn').click();
  check($('startBtn').textContent === '▶ Resume', 'button becomes Resume while paused');
  check($('statStatus').textContent === 'Paused', 'status shows Paused');
  check($('stepBtn').disabled === false, 'step enabled while paused');

  const compsAtPause = $('statComparisons').textContent;
  await new Promise((r) => setTimeout(r, 100));
  check($('statComparisons').textContent === compsAtPause, 'no progress while paused');

  $('startBtn').click();
  check($('startBtn').textContent === '⏸ Pause', 'button back to Pause after resume');

  // Crank speed to max to finish fast.
  $('speedSlider').value = '100';
  $('speedSlider').dispatchEvent(new window.Event('input'));

  const deadline = Date.now() + 30000;
  while ($('statStatus').textContent !== '✓ Sorted!' && Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, 50));
  }
  check($('statStatus').textContent === '✓ Sorted!', 'animated run completes');
  check(isSorted(barHeights()), 'animated run ends sorted');
  check($('sizeSlider').disabled === false, 'controls re-enabled after finish');

  // --- New array & sliders ---
  $('sizeSlider').value = '100';
  $('sizeSlider').dispatchEvent(new window.Event('input'));
  check($('barContainer').children.length === 100, 'size slider regenerates 100 bars');
  check($('sizeVal').textContent === '100', 'size output updates');

  $('patternSelect').value = 'reversed';
  $('patternSelect').dispatchEvent(new window.Event('change'));
  const h = barHeights();
  check(h.every((v, i) => i === 0 || h[i - 1] >= v), 'reversed pattern produces descending bars');

  $('patternSelect').value = 'few';
  $('patternSelect').dispatchEvent(new window.Event('change'));
  const uniq = new Set(barHeights());
  check(uniq.size <= 5, `few-unique pattern yields <=5 distinct heights (got ${uniq.size})`);

  $('shuffleBtn').click();
  check($('barContainer').children.length === 100, 'shuffle keeps size');

  // --- Code line highlighting during a step ---
  $('patternSelect').value = 'random';
  $('patternSelect').dispatchEvent(new window.Event('change'));
  $('stepBtn').click();
  const active = window.document.querySelectorAll('.code-line.active');
  check(active.length >= 1, 'stepping highlights at least one code line');

  if (failures === 0) {
    console.log('✓ All UI smoke checks passed.');
    process.exit(0);
  } else {
    console.error(`✗ ${failures} UI check(s) failed.`);
    process.exit(1);
  }
})().catch((e) => {
  console.error('✗ Smoke test crashed:', e);
  process.exit(1);
});
