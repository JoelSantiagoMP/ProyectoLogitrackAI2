import { randomUUID } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { isInitializeRequest } from "@modelcontextprotocol/sdk/types.js";
import { registerLogitrackTools } from "./tools.js";

function loadEnvFile() {
  const envPath = join(dirname(fileURLToPath(import.meta.url)), "..", ".env");
  if (!existsSync(envPath)) {
    return;
  }
  for (const line of readFileSync(envPath, "utf8").split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }
    const eq = trimmed.indexOf("=");
    if (eq <= 0) {
      continue;
    }
    const key = trimmed.slice(0, eq).trim();
    const value = trimmed.slice(eq + 1).trim();
    if (!(key in process.env)) {
      process.env[key] = value;
    }
  }
}

loadEnvFile();

function createServer() {
  const server = new McpServer({
    name: "logitrack-mcp",
    version: "1.0.0",
  });
  registerLogitrackTools(server);
  return server;
}

async function startStdio() {
  const server = createServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

async function startHttp() {
  const port = Number(process.env.MCP_PORT || 3100);
  const path = process.env.MCP_PATH || "/mcp";
  const app = express();
  app.use(express.json({ limit: "2mb" }));

  const transports = new Map();

  app.post(path, async (req, res) => {
    const sessionId = req.headers["mcp-session-id"];
    try {
      let transport = sessionId ? transports.get(sessionId) : undefined;
      if (!transport && isInitializeRequest(req.body)) {
        transport = new StreamableHTTPServerTransport({
          sessionIdGenerator: () => randomUUID(),
          onsessioninitialized: (id) => {
            transports.set(id, transport);
          },
        });
        transport.onclose = () => {
          if (transport.sessionId) {
            transports.delete(transport.sessionId);
          }
        };
        const server = createServer();
        await server.connect(transport);
      }
      if (!transport) {
        res.status(400).json({ error: "Sesión MCP inválida o ausente" });
        return;
      }
      await transport.handleRequest(req, res, req.body);
    } catch (error) {
      if (!res.headersSent) {
        res.status(500).json({ error: error instanceof Error ? error.message : String(error) });
      }
    }
  });

  const handleSession = async (req, res) => {
    const sessionId = req.headers["mcp-session-id"];
    const transport = sessionId ? transports.get(sessionId) : undefined;
    if (!transport) {
      res.status(400).end("Sesión MCP inválida");
      return;
    }
    await transport.handleRequest(req, res);
  };

  app.get(path, handleSession);
  app.delete(path, handleSession);

  app.get("/health", (_req, res) => {
    res.json({ status: "ok", tools: 6, mysql: false });
  });

  app.listen(port, () => {
    console.error(`MCP LogiTrack HTTP en http://localhost:${port}${path}`);
  });
}

const transport = (process.env.MCP_TRANSPORT || "http").toLowerCase();
if (transport === "stdio") {
  await startStdio();
} else {
  await startHttp();
}
