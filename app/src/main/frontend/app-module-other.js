// Reached only from ModuleOtherView (vaadin/flow#27425). ModuleCallsView calls it through reflection, row F1, so its chunk does not register it.
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module-other'] = (evals['app-module-other'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'app-module-other' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}
