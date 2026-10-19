const json = (data, status = 200) => new Response(JSON.stringify(data, null, 2), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "content-type, x-publish-secret",
    "access-control-allow-methods": "GET, POST, OPTIONS"
  }
});

const memory = globalThis.__LOTOSMS_MEMORY__ || (globalThis.__LOTOSMS_MEMORY__ = new Map());

function getStore(env) {
  return env.RESULTS_KV || null;
}

async function saveResult(env, result) {
  const store = getStore(env);
  if (store) {
    await store.put(`draw:${result.drawId}`, JSON.stringify(result));
    await store.put(`latest:${result.game}`, JSON.stringify(result));
    await store.put("latest:any", JSON.stringify(result));
  } else {
    memory.set(`draw:${result.drawId}`, result);
    memory.set(`latest:${result.game}`, result);
    memory.set("latest:any", result);
  }
}

async function readResult(env, key) {
  const store = getStore(env);
  if (store) {
    const raw = await store.get(key);
    return raw ? JSON.parse(raw) : null;
  }
  return memory.get(key) || null;
}

async function notifyDevices(env, result) {
  if (!env.FCM_ENDPOINT || !env.FCM_AUTH_TOKEN) return { sent: false, reason: "FCM not configured" };

  const response = await fetch(env.FCM_ENDPOINT, {
    method: "POST",
    headers: {
      "authorization": `Bearer ${env.FCM_AUTH_TOKEN}`,
      "content-type": "application/json"
    },
    body: JSON.stringify({
      drawId: result.drawId,
      game: result.game,
      title: result.title,
      message: result.message
    })
  });

  return { sent: response.ok, status: response.status };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") return json({ ok: true });

    if (request.method === "GET" && url.pathname === "/health") {
      return json({
        ok: true,
        service: "LotoSMS API",
        version: "0.1.0",
        storage: env.RESULTS_KV ? "KV" : "memory-fallback"
      });
    }

    if (request.method === "GET" && url.pathname === "/results/latest") {
      const game = url.searchParams.get("game")?.trim().toLowerCase();
      const result = await readResult(env, game ? `latest:${game}` : "latest:any");
      return result ? json(result) : json({ ok: true, result: null });
    }

    if (request.method === "POST" && url.pathname === "/publish") {
      if (env.PUBLISH_SECRET && request.headers.get("x-publish-secret") !== env.PUBLISH_SECRET) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }

      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false, error: "invalid_json" }, 400);
      }

      const drawId = String(body.drawId || "").trim();
      const game = String(body.game || "").trim().toLowerCase();
      const title = String(body.title || "").trim();
      const message = String(body.message || "").trim();

      if (!drawId || !game || !title || !message) {
        return json({ ok: false, error: "drawId, game, title and message are required" }, 400);
      }

      const existing = await readResult(env, `draw:${drawId}`);
      if (existing) return json({ ok: true, duplicate: true, result: existing });

      const result = {
        drawId,
        game,
        title,
        message,
        publishedAt: new Date().toISOString()
      };

      await saveResult(env, result);
      const notification = await notifyDevices(env, result);

      return json({ ok: true, duplicate: false, notification, result });
    }

    return json({
      ok: true,
      service: "LotoSMS API",
      endpoints: ["GET /health", "GET /results/latest?game=euromilhoes", "POST /publish"]
    });
  }
};
