// Imported with a query, ./app-module-query.js?v=2 (vaadin/flow#27425, row M12).
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module-query'] = (evals['app-module-query'] || 0) + 1;

// Sets up the element like a connector does, and records the call, so that
// a call after this one can tell whether it ran first
export function initLazy(element) {
  element.$connector = { from: 'app-module-query' };
  element.dataset.order = ((element.dataset.order ?? '') + ' init').trim();
}
