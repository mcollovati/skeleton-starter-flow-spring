// The module of org.vaadin.example.modules.ConnectorJs (vaadin/flow#27425).
// J1 switches between connector-v1.js and connector-v2.js; J3 edits
// connector-v1.js.
export function initLazy(element) {
  element.$connector = { version: 'v2' };
}
