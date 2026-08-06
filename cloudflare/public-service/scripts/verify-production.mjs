const defaultBaseUrl = "https://whitelist-monitor-tg-relay.carkov195.workers.dev";
const baseUrl = (process.env.PUBLIC_SERVICE_BASE_URL ?? defaultBaseUrl).replace(/\/+$/, "");

const healthResponse = await fetch(`${baseUrl}/health`, {
  headers: { Accept: "application/json" },
});

if (!healthResponse.ok) {
  throw new Error(`Health check failed with HTTP ${healthResponse.status}`);
}

const health = await healthResponse.json();
if (health.status !== "ok" || !Array.isArray(health.capabilities)) {
  throw new Error("Production Worker uses a legacy /health response");
}
if (!health.capabilities.includes("service-sync")) {
  throw new Error(`Production Worker ${health.revision ?? "unknown"} does not advertise service-sync`);
}

const routeResponse = await fetch(`${baseUrl}/api/v1/installations/me/service-sync`, {
  method: "GET",
  headers: { Accept: "application/json" },
});

if (routeResponse.status === 404) {
  throw new Error("Production Worker does not contain the service-sync route");
}
if (routeResponse.status !== 405) {
  throw new Error(`Expected service-sync GET probe to return HTTP 405, got ${routeResponse.status}`);
}

console.log(`Production Worker revision ${health.revision} supports service-sync`);
