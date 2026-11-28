// In the add-on jar: the module of ModuleBox.ModuleBoxJs (vaadin/flow#27425, row M5).
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['acme-module'] = (evals['acme-module'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'acme-module' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}
