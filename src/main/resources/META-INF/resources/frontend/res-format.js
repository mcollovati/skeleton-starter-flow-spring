// A frontend file on the class path (META-INF/resources/frontend), handled
// like one in a jar: the build imports it from generated/jar-resources. M4
// checks that a hotswap resolves it the same way (vaadin/flow#27423).
export function format(text) {
  return 'res(' + text + ')';
}
