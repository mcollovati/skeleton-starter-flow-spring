// Imported only through @JsDefinitionImport (vaadin/flow#27423), so the
// production build puts it in a chunk that is loaded on the first call. The
// counter shows that the module is evaluated once, however many definitions
// import it.
window.acmeFormatLoads = (window.acmeFormatLoads || 0) + 1;

export default function format(text) {
  return '[' + text + ']';
}

export function upper(text) {
  return String(text).toUpperCase();
}

export const version = 'format-1';
