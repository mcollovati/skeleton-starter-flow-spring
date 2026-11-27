// Imported only by ReflectedImportJs, which the application calls through
// reflection (vaadin/flow#27078 with vaadin/flow#27423).
export function replaceWith(element, text) {
  element.textContent = text + ' (acme-reflected-import-module)';
}
