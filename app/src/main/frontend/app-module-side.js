// Both @JsModule and @JsDefinitionImport of SideEffectJs (vaadin/flow#27425, row M9): must be evaluated once.
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module-side'] = (evals['app-module-side'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'app-module-side' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}
