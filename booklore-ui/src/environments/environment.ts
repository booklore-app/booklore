// Direct access to the ng serve dev port (4200) talks to the backend on its own
// port (6060) via CORS. Anything else (accessed through a reverse proxy on 80/443)
// stays same-origin and lets the proxy's /api + /ws routing do the work - the
// backend has no TLS listener of its own, so a proxied HTTPS page can't reach it
// directly on 6060.
const isDirectDevAccess = window.location.port === '4200';

export const environment = {
  production: false,
  API_CONFIG: {
    BASE_URL: isDirectDevAccess
      ? `${window.location.protocol}//${window.location.hostname}:6060`
      : window.location.origin,
    BROKER_URL: isDirectDevAccess
      ? `ws://${window.location.hostname}:6060/ws`
      : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
  },
};
