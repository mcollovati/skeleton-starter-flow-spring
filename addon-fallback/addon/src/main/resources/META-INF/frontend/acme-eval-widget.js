// ACME add-on web component that evaluates an expression from an attribute.
// Under strict CSP (no 'unsafe-eval') the browser throws an EvalError here.
class AcmeEvalWidget extends HTMLElement {
  connectedCallback() {
    try {
      const fn = new Function('return (' + this.getAttribute('expr') + ')');
      this.textContent = 'eval result: ' + fn();
    } catch (e) {
      this.textContent = 'eval refused by the browser: ' + e.name;
    }
  }
}
customElements.define('acme-eval-widget', AcmeEvalWidget);
