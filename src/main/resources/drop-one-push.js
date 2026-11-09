// Drops ONE message that the server pushes over the push WebSocket (the 6th
// one), as a proxy restart or a network problem does. The client then sees a
// gap in the server sync ids and asks the server to resynchronize.
// Open the page with ?nodrop to keep every message.
(function () {
  if (location.search.includes('nodrop')) {
    return;
  }
  const DROP = 6;
  const NativeWebSocket = window.WebSocket;
  let received = 0;
  let dropped = false;
  window.WebSocket = function (url, protocols) {
    const socket = protocols === undefined
      ? new NativeWebSocket(url)
      : new NativeWebSocket(url, protocols);
    if (!String(url).includes('VAADIN/push')) {
      return socket;
    }
    const descriptor = Object.getOwnPropertyDescriptor(
      NativeWebSocket.prototype, 'onmessage');
    Object.defineProperty(socket, 'onmessage', {
      configurable: true,
      get() {
        return descriptor.get.call(socket);
      },
      set(handler) {
        descriptor.set.call(socket, function (event) {
          if (String(event.data).includes('"syncId"')) {
            received++;
            if (!dropped && received === DROP) {
              dropped = true;
              console.warn('drop-one-push.js: dropped pushed message', event.data);
              return;
            }
          }
          return handler.call(this, event);
        });
      },
    });
    return socket;
  };
  window.WebSocket.prototype = NativeWebSocket.prototype;
  window.WebSocket.CONNECTING = NativeWebSocket.CONNECTING;
  window.WebSocket.OPEN = NativeWebSocket.OPEN;
  window.WebSocket.CLOSING = NativeWebSocket.CLOSING;
  window.WebSocket.CLOSED = NativeWebSocket.CLOSED;
})();
