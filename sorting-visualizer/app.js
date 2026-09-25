/*
 * app.js — UI controller for the Java Sorting Visualizer.
 * Depends on algorithms.js (window.SORTING).
 *
 * The sorting algorithms themselves live in algorithms.js as generators that
 * yield step events; this file consumes those events to animate the bars and
 * highlight the matching Java source lines.
 */
'use strict';

(function () {
  const { JAVA_CODE, META, GENERATORS, ORDER } = window.SORTING;

  /* ---------------- DOM references ---------------- */
  const el = {
    tabs: document.getElementById('algoTabs'),
    shuffleBtn: document.getElementById('shuffleBtn'),
    startBtn: document.getElementById('startBtn'),
    stepBtn: document.getElementById('stepBtn'),
    resetBtn: document.getElementById('resetBtn'),
    sizeSlider: document.getElementById('sizeSlider'),
    sizeVal: document.getElementById('sizeVal'),
    speedSlider: document.getElementById('speedSlider'),
    speedVal: document.getElementById('speedVal'),
    patternSelect: document.getElementById('patternSelect'),
    statStatus: document.getElementById('statStatus'),
    statComparisons: document.getElementById('statComparisons'),
    statSwaps: document.getElementById('statSwaps'),
    statTime: document.getElementById('statTime'),
    statSize: document.getElementById('statSize'),
    barContainer: document.getElementById('barContainer'),
    codeTitle: document.getElementById('codeTitle'),
    codeBlock: document.getElementById('codeBlock'),
    codeContent: document.getElementById('codeContent'),
    complexityCard: document.getElementById('complexityCard'),
  };

  /* ---------------- State ---------------- */
  const state = {
    algo: 'bubble',
    array: [],
    initialArray: null,   // snapshot taken when a sort starts, for Reset
    gen: null,
    running: false,
    paused: false,
    finished: false,
    size: 60,
    speed: 50,
    pattern: 'random',
    comparisons: 0,
    swaps: 0,
    elapsed: 0,
    t0: 0,
    timer: null,          // setTimeout handle for the animation loop
    clock: null,          // setInterval handle for the elapsed clock
  };

  let bars = [];               // bar DOM elements, parallel to state.array
  const transientBars = new Set(); // bars with a temporary highlight class
  const dirtyBars = new Set();     // bars whose height needs re-rendering
  let activeLines = [];            // currently highlighted code line elements

  /* ---------------- Java syntax highlighting ---------------- */
  const JAVA_TOKEN =
    /(\/\/.*$)|("(?:[^"\\]|\\.)*")|\b(\d+)\b|\b(public|private|protected|static|final|void|int|long|boolean|new|if|else|for|while|do|return|break|continue|class|this)\b/g;

  function highlightJavaLine(line) {
    const escaped = line
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    return escaped.replace(JAVA_TOKEN, (m, comment, str, num, kw) => {
      if (comment !== undefined) return `<span class="tok-comment">${comment}</span>`;
      if (str !== undefined) return `<span class="tok-string">${str}</span>`;
      if (num !== undefined) return `<span class="tok-num">${num}</span>`;
      if (kw !== undefined) return `<span class="tok-kw">${kw}</span>`;
      return m;
    });
  }

  /* ---------------- Code panel ---------------- */
  function renderCodePanel() {
    const meta = META[state.algo];
    el.codeTitle.textContent = meta.name;
    const lines = JAVA_CODE[state.algo].split('\n');
    el.codeContent.innerHTML = lines
      .map(
        (src, idx) =>
          `<div class="code-line" data-line="${idx + 1}">` +
          `<span class="line-no">${idx + 1}</span>` +
          `<span class="line-src">${highlightJavaLine(src) || ' '}</span></div>`
      )
      .join('');
    el.codeBlock.scrollTop = 0;
    activeLines = [];
    renderComplexityCard(meta);
  }

  function renderComplexityCard(meta) {
    el.complexityCard.innerHTML =
      `<p>${meta.description}</p>` +
      `<div class="complexity-grid">` +
      `<div class="complexity-cell"><span class="ck">Best</span><span class="cv">${meta.best}</span></div>` +
      `<div class="complexity-cell"><span class="ck">Average</span><span class="cv">${meta.average}</span></div>` +
      `<div class="complexity-cell"><span class="ck">Worst</span><span class="cv">${meta.worst}</span></div>` +
      `<div class="complexity-cell"><span class="ck">Space</span><span class="cv">${meta.space}</span></div>` +
      `<div class="complexity-cell"><span class="ck">Stable</span><span class="cv">${meta.stable}</span></div>` +
      `</div>`;
  }

  function highlightCode(lines) {
    for (const node of activeLines) node.classList.remove('active');
    activeLines = [];
    for (const ln of lines) {
      const node = el.codeContent.querySelector(`[data-line="${ln}"]`);
      if (node) {
        node.classList.add('active');
        activeLines.push(node);
      }
    }
    // Keep the highlighted line visible (only scroll when it drifts out of view).
    if (activeLines.length > 0) {
      const first = activeLines[0];
      const top = first.offsetTop;
      const viewTop = el.codeBlock.scrollTop;
      const viewBottom = viewTop + el.codeBlock.clientHeight;
      if (top < viewTop + 20 || top > viewBottom - 40) {
        el.codeBlock.scrollTop = Math.max(0, top - el.codeBlock.clientHeight / 2);
      }
    }
  }

  /* ---------------- Array generation ---------------- */
  function randInt(lo, hi) {
    return lo + Math.floor(Math.random() * (hi - lo + 1));
  }

  function generateArray(size, pattern) {
    let arr = Array.from({ length: size }, () => randInt(5, 100));
    if (pattern === 'nearly') {
      arr.sort((a, b) => a - b);
      const swaps = Math.max(1, Math.round(size * 0.05));
      for (let k = 0; k < swaps; k++) {
        const i = randInt(0, size - 1);
        const j = randInt(0, size - 1);
        [arr[i], arr[j]] = [arr[j], arr[i]];
      }
    } else if (pattern === 'reversed') {
      arr.sort((a, b) => b - a);
    } else if (pattern === 'few') {
      const vals = [15, 30, 50, 70, 90];
      arr = arr.map(() => vals[randInt(0, vals.length - 1)]);
    }
    return arr;
  }

  /* ---------------- Bar rendering ---------------- */
  function renderBars() {
    el.barContainer.innerHTML = '';
    el.barContainer.style.gap = state.size > 90 ? '1px' : '2px';
    bars = state.array.map((v) => {
      const d = document.createElement('div');
      d.className = 'bar';
      d.style.height = v + '%';
      el.barContainer.appendChild(d);
      return d;
    });
    transientBars.clear();
    dirtyBars.clear();
  }

  function setTransient(i, cls) {
    bars[i].classList.add(cls);
    transientBars.add(i);
  }

  function clearTransient() {
    for (const i of transientBars) {
      bars[i].classList.remove('comparing', 'swapping', 'pivot');
    }
    transientBars.clear();
  }

  function renderDirty() {
    for (const i of dirtyBars) {
      bars[i].style.height = state.array[i] + '%';
    }
    dirtyBars.clear();
  }

  /* ---------------- Consuming generator events ---------------- */
  function applyEvent(ev) {
    highlightCode(ev.lines || []);
    switch (ev.type) {
      case 'compare':
        state.comparisons++;
        setTransient(ev.i, 'comparing');
        setTransient(ev.j, 'comparing');
        break;
      case 'swap':
        state.swaps++;
        setTransient(ev.i, 'swapping');
        setTransient(ev.j, 'swapping');
        dirtyBars.add(ev.i);
        dirtyBars.add(ev.j);
        break;
      case 'set':
        state.swaps++; // counts array writes (shifts / merge write-backs)
        setTransient(ev.i, 'swapping');
        dirtyBars.add(ev.i);
        break;
      case 'pivot':
        setTransient(ev.i, 'pivot');
        break;
      case 'markSorted': {
        const b = bars[ev.i];
        b.classList.remove('comparing', 'swapping', 'pivot');
        b.classList.add('sorted');
        break;
      }
      // 'note' events only highlight code — no bar changes.
    }
  }

  /* ---------------- Stats & status ---------------- */
  function formatElapsed(ms) {
    return (ms / 1000).toFixed(1) + 's';
  }

  function updateStats() {
    el.statComparisons.textContent = state.comparisons.toLocaleString();
    el.statSwaps.textContent = state.swaps.toLocaleString();
    el.statSize.textContent = String(state.size);
    el.statTime.textContent = formatElapsed(state.elapsed);
  }

  function setStatus(text, cls) {
    el.statStatus.textContent = text;
    el.statStatus.className = 'stat-value' + (cls ? ' ' + cls : '');
  }

  function startClock() {
    stopClock();
    state.t0 = performance.now() - state.elapsed;
    state.clock = setInterval(() => {
      state.elapsed = performance.now() - state.t0;
      el.statTime.textContent = formatElapsed(state.elapsed);
    }, 100);
  }

  function stopClock() {
    if (state.clock) {
      clearInterval(state.clock);
      state.clock = null;
    }
  }

  /* ---------------- Animation runner ---------------- */
  function speedParams() {
    const s = state.speed; // 1..100
    if (s <= 55) {
      // Delay-driven: one event per tick, delay shrinks as speed rises.
      const delay = Math.round(420 * Math.pow(0.92, s - 1)); // 420ms … ~4ms
      return { delay, batch: 1 };
    }
    // Batch-driven: no delay, many events per ~16ms frame.
    const batch = Math.min(140, 1 + Math.round(Math.pow(s - 55, 1.35) / 1.5));
    return { delay: 0, batch };
  }

  function tick() {
    if (!state.running || state.paused) return;
    const { delay, batch } = speedParams();
    clearTransient();
    let done = false;
    const steps = delay > 0 ? 1 : batch;
    for (let k = 0; k < steps; k++) {
      const r = state.gen.next();
      if (r.done) {
        done = true;
        break;
      }
      applyEvent(r.value);
    }
    renderDirty();
    updateStats();
    if (done) {
      finishSort();
      return;
    }
    state.timer = setTimeout(tick, delay > 0 ? delay : 16);
  }

  function singleStep() {
    if (!state.running) {
      startSort(true); // start, but stay paused
    }
    if (!state.running || !state.paused || state.finished) return;
    clearTransient();
    const r = state.gen.next();
    if (r.done) {
      finishSort();
      return;
    }
    applyEvent(r.value);
    renderDirty();
    updateStats();
  }

  function startSort(stayPaused = false) {
    if (state.finished) resetToInitial();
    state.initialArray = [...state.array];
    state.gen = GENERATORS[state.algo](state.array);
    state.comparisons = 0;
    state.swaps = 0;
    state.elapsed = 0;
    state.running = true;
    state.paused = stayPaused;
    state.finished = false;
    clearTransient();
    for (const b of bars) b.classList.remove('sorted');
    highlightCode([]);
    startClock();
    updateStartBtn();
    setControlsForRunning();
    setStatus(stayPaused ? 'Paused' : 'Sorting…', stayPaused ? '' : 'status-sorting');
    if (!stayPaused) tick();
  }

  function pauseSort() {
    state.paused = true;
    clearTimeout(state.timer);
    stopClock();
    updateStartBtn();
    setControlsForRunning();
    setStatus('Paused');
  }

  function resumeSort() {
    state.paused = false;
    startClock();
    updateStartBtn();
    setControlsForRunning();
    setStatus('Sorting…', 'status-sorting');
    tick();
  }

  function finishSort() {
    state.running = false;
    state.paused = false;
    state.finished = true;
    clearTimeout(state.timer);
    stopClock();
    clearTransient();
    highlightCode([]);
    // Green sweep across the bars as a victory lap.
    const step = Math.max(2, Math.floor(500 / bars.length));
    bars.forEach((b, i) => setTimeout(() => b.classList.add('sorted'), i * step));
    setStatus('✓ Sorted!', 'status-done');
    updateStartBtn();
    setControlsForIdle();
  }

  function stopSort() {
    state.running = false;
    state.paused = false;
    state.finished = false;
    state.gen = null;
    clearTimeout(state.timer);
    stopClock();
    clearTransient();
  }

  function resetToInitial() {
    if (state.initialArray) {
      state.array = [...state.initialArray];
    }
    state.comparisons = 0;
    state.swaps = 0;
    state.elapsed = 0;
    renderBars();
    highlightCode([]);
    updateStats();
  }

  function resetRun() {
    stopSort();
    resetToInitial();
    setStatus('Ready');
    updateStartBtn();
    setControlsForIdle();
  }

  function newArray() {
    stopSort();
    state.array = generateArray(state.size, state.pattern);
    state.initialArray = null;
    state.comparisons = 0;
    state.swaps = 0;
    state.elapsed = 0;
    renderBars();
    highlightCode([]);
    updateStats();
    setStatus('Ready');
    updateStartBtn();
    setControlsForIdle();
  }

  function switchAlgorithm(algo) {
    if (algo === state.algo) return;
    stopSort();
    state.algo = algo;
    for (const tab of el.tabs.children) {
      tab.classList.toggle('active', tab.dataset.algo === algo);
    }
    renderCodePanel();
    resetToInitial(); // restore the pre-sort array (if any) and clear stats
    setStatus('Ready');
    updateStartBtn();
    setControlsForIdle();
  }

  /* ---------------- Button / control states ---------------- */
  function updateStartBtn() {
    if (!state.running) {
      el.startBtn.textContent = '▶ Start';
    } else if (state.paused) {
      el.startBtn.textContent = '▶ Resume';
    } else {
      el.startBtn.textContent = '⏸ Pause';
    }
  }

  function setControlsForRunning() {
    el.shuffleBtn.disabled = true;
    el.sizeSlider.disabled = true;
    el.patternSelect.disabled = true;
    el.stepBtn.disabled = !state.paused;
    for (const tab of el.tabs.children) tab.disabled = true;
  }

  function setControlsForIdle() {
    el.shuffleBtn.disabled = false;
    el.sizeSlider.disabled = false;
    el.patternSelect.disabled = false;
    el.stepBtn.disabled = state.finished;
    for (const tab of el.tabs.children) tab.disabled = false;
  }

  /* ---------------- Wiring ---------------- */
  function buildTabs() {
    for (const algo of ORDER) {
      const btn = document.createElement('button');
      btn.className = 'algo-tab' + (algo === state.algo ? ' active' : '');
      btn.dataset.algo = algo;
      btn.textContent = META[algo].name;
      btn.addEventListener('click', () => switchAlgorithm(algo));
      el.tabs.appendChild(btn);
    }
  }

  el.startBtn.addEventListener('click', () => {
    if (!state.running) startSort();
    else if (state.paused) resumeSort();
    else pauseSort();
  });
  el.shuffleBtn.addEventListener('click', newArray);
  el.resetBtn.addEventListener('click', resetRun);
  el.stepBtn.addEventListener('click', singleStep);

  el.sizeSlider.addEventListener('input', () => {
    state.size = Number(el.sizeSlider.value);
    el.sizeVal.textContent = el.sizeSlider.value;
    newArray();
  });
  el.speedSlider.addEventListener('input', () => {
    state.speed = Number(el.speedSlider.value);
    el.speedVal.textContent = el.speedSlider.value;
  });
  el.patternSelect.addEventListener('change', () => {
    state.pattern = el.patternSelect.value;
    newArray();
  });

  document.addEventListener('keydown', (e) => {
    const tag = document.activeElement && document.activeElement.tagName;
    if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
    if (e.code === 'Space') {
      e.preventDefault();
      el.startBtn.click();
    }
  });

  /* ---------------- Init ---------------- */
  buildTabs();
  renderCodePanel();
  state.array = generateArray(state.size, state.pattern);
  renderBars();
  updateStats();
  setStatus('Ready');
  setControlsForIdle();
  el.stepBtn.disabled = false; // stepping from idle starts a paused run
})();
