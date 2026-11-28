// Reached from ModuleCallsView and ModuleOtherView (vaadin/flow#27425, row
// M10). Both chunks register it, so it must be evaluated once and keep its
// state across the two views.
const evals = (window.__moduleEvals = window.__moduleEvals || {});
evals['app-module-shared'] = (evals['app-module-shared'] || 0) + 1;

let count = 0;

export function increment() {
  count++;
  return count;
}
