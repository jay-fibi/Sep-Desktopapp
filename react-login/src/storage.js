/* Thin, failure-tolerant wrapper around localStorage.
 *
 * Storage can throw (private mode, disabled cookies, file:// pages), so every
 * call is guarded and simply degrades to "nothing remembered" instead of
 * breaking the form.
 */

function safeStorage() {
  try {
    return typeof window !== "undefined" ? window.localStorage : null;
  } catch (error) {
    console.warn(`localStorage unavailable (${error.message})`);
    return null;
  }
}

export function readStored(key) {
  const store = safeStorage();
  if (!store) {
    return null;
  }
  try {
    return store.getItem(key);
  } catch (error) {
    console.warn(`localStorage unavailable (${error.message})`);
    return null;
  }
}

export function writeStored(key, value) {
  const store = safeStorage();
  if (!store) {
    return;
  }
  try {
    store.setItem(key, value);
  } catch (error) {
    console.warn(`localStorage unavailable (${error.message})`);
  }
}

export function removeStored(key) {
  const store = safeStorage();
  if (!store) {
    return;
  }
  try {
    store.removeItem(key);
  } catch (error) {
    console.warn(`localStorage unavailable (${error.message})`);
  }
}

export function readJson(key) {
  const raw = readStored(key);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}
