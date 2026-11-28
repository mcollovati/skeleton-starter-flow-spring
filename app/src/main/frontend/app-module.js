// The module of AppModuleJs (vaadin/flow#27425): rows M1-M4, M7, M8, M11.
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module'] = (evals['app-module'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'app-module' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}

export function format(text) {
  return '<' + text + '>';
}
