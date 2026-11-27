// A module in the frontend folder of the project, imported through
// @JsDefinitionImport (vaadin/flow#27423) by AppImports.ConnectorJs, for
// row I13 (the order of the calls).
export function init(element) {
  element.$connector = { ready: true };
  element.dataset.order = (element.dataset.order + ' init').trim();
}
