export const environment = {
  production: true,
  API_CONFIG: {
    BASE_URL: `${window.location.protocol}//${window.location.hostname}:${window.location.port}`,
    BROKER_URL: `ws://${window.location.hostname}:${window.location.port}/ws`,
  },
};
