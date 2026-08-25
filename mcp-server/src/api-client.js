/**
 * Cliente HTTP autenticado contra Spring Boot.
 * Solo reenvía peticiones; no calcula stock, KPIs ni transiciones de orden.
 */

const API_BASE_URL = (process.env.LOGITRACK_API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");
const USERNAME = process.env.LOGITRACK_USERNAME || "agente_logitrack";
const PASSWORD = process.env.LOGITRACK_PASSWORD || "123456";

let cachedToken = null;

async function login() {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
  });
  const body = await response.text();
  if (!response.ok) {
    throw new Error(`Login AGENTE falló (${response.status}): ${body}`);
  }
  const json = JSON.parse(body);
  const token = json.token || json.accessToken;
  if (!token) {
    throw new Error("Login AGENTE no devolvió token JWT");
  }
  cachedToken = token;
  return token;
}

async function authorizedFetch(path, options = {}, retried = false) {
  const token = cachedToken || (await login());
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {}),
    },
  });

  if (response.status === 401 && !retried) {
    cachedToken = null;
    return authorizedFetch(path, options, true);
  }

  const text = await response.text();
  let parsed;
  try {
    parsed = text ? JSON.parse(text) : null;
  } catch {
    parsed = text;
  }

  if (!response.ok) {
    const detail = typeof parsed === "string" ? parsed : JSON.stringify(parsed);
    const error = new Error(`API ${options.method || "GET"} ${path} → ${response.status}: ${detail}`);
    error.status = response.status;
    throw error;
  }
  return parsed;
}

export function get(path) {
  return authorizedFetch(path, { method: "GET" });
}

export function post(path, body) {
  return authorizedFetch(path, { method: "POST", body: JSON.stringify(body) });
}
