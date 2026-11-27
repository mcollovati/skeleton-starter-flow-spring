// Same default export name as acme-format.js with another result: the same
// JavaScript with other imports must be another function (vaadin/flow#27423).
export default function format(text) {
  return '<' + text + '>';
}
