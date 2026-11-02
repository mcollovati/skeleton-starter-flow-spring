// Stands in for a large web component that most users never open. It must end
// up in its own chunk, loaded only when DynamicImportView imports it.
console.info('report-editor module evaluated');
window.reportEditorModuleEvaluated = true;

class ReportEditor extends HTMLElement {
  constructor() {
    super();
    // Not connected yet: Flow created the element after this class was
    // defined, which is what the blocking dynamic import guarantees. An element
    // created before the definition is upgraded later, when already connected.
    this.definedBeforeCreation = !this.isConnected;
  }

  connectedCallback() {
    this.textContent =
      `report-editor for "${this.getAttribute('report')}"` +
      ` - defined before Flow created it: ${this.definedBeforeCreation ? 'yes' : 'no'}`;
  }
}

customElements.define('report-editor', ReportEditor);
