// Imported as a namespace through @JsDefinitionImport (vaadin/flow#27423).
// Also exports "version", like acme-format.js, so only a namespace can give
// both to one definition.
window.acmeLabelLoads = (window.acmeLabelLoads || 0) + 1;

export default 'label-default';

export function label(text) {
  return 'label: ' + text;
}

export const version = 'label-1';
