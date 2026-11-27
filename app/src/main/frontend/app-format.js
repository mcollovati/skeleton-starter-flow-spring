// A module in the frontend folder of the project, imported through
// @JsDefinitionImport (vaadin/flow#27423) by org.vaadin.example.imports.
// Change the braces to check that the next build rebuilds the bundle.
window.appFormatLoads = (window.appFormatLoads || 0) + 1;

export default function format(text) {
  return '{' + text + '}';
}

export function shout(text) {
  return String(text).toUpperCase() + '!';
}
