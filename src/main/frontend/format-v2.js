// Imported by the definitions of org.vaadin.example.imports through
// @JsDefinitionImport (vaadin/flow#27423). M1-M3 switch between
// format-v1.js and format-v2.js; M5 edits this file.
export default function format(text) {
  return 'v2(' + text + ')';
}

export function shout(text) {
  return 'v2 ' + String(text).toUpperCase() + '!';
}
