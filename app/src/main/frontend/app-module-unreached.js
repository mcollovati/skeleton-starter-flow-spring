// Imported by UnreachedModuleJs, which no route reaches (vaadin/flow#27425, row F2).
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module-unreached'] = (evals['app-module-unreached'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'app-module-unreached' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}
