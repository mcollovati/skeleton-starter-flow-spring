// Imported without the extension, ./app-module-noext (vaadin/flow#27425, row M13).
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module-noext'] = (evals['app-module-noext'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'app-module-noext' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}
