const json = (data, status = 200) => new Response(JSON.stringify(data, null, 2), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "content-type, x-publish-secret",
    "access-control-allow-methods": "GET, POST, OPTIONS"
  }
});



const OFFICIAL_SOURCES = [
  {
    game: "euromilhoes",
    title: "Euromilhões",
    url: "https://www.jogossantacasa.pt/web/SCCartazResult/euroMilhoes",
    numbers: 5,
    extras: 2,
    extraLabel: "Estrelas"
  },
  {
    game: "totoloto",
    title: "Totoloto",
    url: "https://www.jogossantacasa.pt/web/SCCartazResult/totolotoNew",
    numbers: 5,
    extras: 1,
    extraLabel: "N.º da Sorte"
  },
  {
    game: "eurodreams",
    title: "EuroDreams",
    url: "https://www.jogossantacasa.pt/web/ResultsBoard/EuroDreams",
    numbers: 6,
    extras: 1,
    extraLabel: "N.º de Sonho"
  }
];

function getStore(env) {
  if (!env.RESULTS_KV) throw new Error("RESULTS_KV binding required");
  return env.RESULTS_KV;
}

async function saveResult(env, result) {
  const store = getStore(env);
  if (store) {
    await store.put(`draw:${result.drawId}`, JSON.stringify(result));
    await store.put(`latest:${result.game}`, JSON.stringify(result));
    await store.put("latest:any", JSON.stringify(result));
  }
}

async function readResult(env, key) {
  const store = getStore(env);
  if (store) {
    const raw = await store.get(key);
    return raw ? JSON.parse(raw) : null;
  }
  return null;
}

async function notifyDevices(env, result) {
  if (!env.FCM_ENDPOINT || !env.FCM_AUTH_TOKEN) {
    return { sent: false, reason: "FCM not configured" };
  }

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

function htmlToText(html) {
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;|&#160;/gi, " ")
    .replace(/&ordm;/gi, "º")
    .replace(/&[a-z]+;/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function extractOfficialResult(source, html) {
  const text = htmlToText(html);
  const contestMatch = text.match(/Sorteio:\s*([0-9]{2,3}\/20[0-9]{2})/i);
  if (!contestMatch) return null;

  const keyIndex = text.toLowerCase().indexOf("chave");
  if (keyIndex < 0) return null;

  const keyArea = text.slice(keyIndex, keyIndex + 500);
  // Regex explícita para manter a extração previsível em cada jogo.
  let match;
  if (source.game === "euromilhoes") {
    match = keyArea.match(/([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s*\+\s*([0-9]{1,2})\s+([0-9]{1,2})/);
  } else if (source.game === "totoloto") {
    match = keyArea.match(/([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s*\+\s*([0-9]{1,2})/);
  } else {
    match = keyArea.match(/([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s+([0-9]{1,2})\s*\+\s*([0-9]{1,2})/);
  }
  if (!match) return null;

  const values = match.slice(1).map(Number);
  const main = values.slice(0, source.numbers);
  const extras = values.slice(source.numbers);
  const ranges = {
    euromilhoes: { main: 50, extra: 12 },
    totoloto: { main: 49, extra: 13 },
    eurodreams: { main: 40, extra: 5 }
  };
  const limits = ranges[source.game];
  if (!limits) return null;
  if (main.length !== source.numbers || extras.length !== source.extras) return null;
  if (new Set(main).size !== main.length || new Set(extras).size !== extras.length) return null;
  if (main.some(n => !Number.isInteger(n) || n < 1 || n > limits.main)) return null;
  if (extras.some(n => !Number.isInteger(n) || n < 1 || n > limits.extra)) return null;
  const contest = contestMatch[1];
  // Only report a draw date if it is explicitly present in the official page.
  const dateArea = text.slice(Math.max(0, contestMatch.index - 100), contestMatch.index + 220);
  const dateMatch = dateArea.match(/\b([0-3]?\d)[\/.-]([01]?\d)[\/.-](20\d{2})\b/);
  const drawDate = dateMatch
    ? [dateMatch[1].padStart(2, "0"), dateMatch[2].padStart(2, "0"), dateMatch[3]].join("/")
    : null;
  const formattedMessage = [
    source.title,
    drawDate ? `Data: ${drawDate}` : "Data: não indicada na fonte",
    `Sorteio n.º ${contest}`,
    `Números: ${main.join(" - ")}`,
    `${source.extraLabel}: ${extras.join(" - ")}`
  ].join("\n");

  return {
    drawId: `${source.game}:${contest}`,
    game: source.game,
    title: `${source.title} — ${contest}`,
    message: formattedMessage,
    drawDate,
    numbers: main,
    extras,
    source: source.url,
    publishedAt: new Date().toISOString()
  };
}

async function refreshOfficialResults(env) {
  const checks = [];

  for (const source of OFFICIAL_SOURCES) {
    try {
      const response = await fetch(source.url, {
        headers: {
          "user-agent": "LotoSMS/1.0 (+result-checker)"
        }
      });

      if (!response.ok) {
        checks.push({ game: source.game, ok: false, status: response.status });
        continue;
      }

      const result = extractOfficialResult(source, await response.text());
      if (!result) {
        checks.push({ game: source.game, ok: false, reason: "parse_failed" });
        continue;
      }

      const existing = await readResult(env, `draw:${result.drawId}`);
      if (existing) {
        if (existing.message !== result.message || existing.drawDate !== result.drawDate) {
          await saveResult(env, result); // Refresh formatting without notifying a second time.
        }
        checks.push({ game: source.game, ok: true, duplicate: true, drawId: result.drawId });
        continue;
      }

      await saveResult(env, result);
      const notification = await notifyDevices(env, result);
      checks.push({ game: source.game, ok: true, duplicate: false, drawId: result.drawId, notification });
    } catch (error) {
      checks.push({ game: source.game, ok: false, reason: String(error?.message || error) });
    }
  }

  return checks;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") return json({ ok: true });

    if (request.method === "GET" && url.pathname === "/health") {
      return json({
        ok: true,
        service: "LotoSMS API",
        version: "0.2.0",
        storage: env.RESULTS_KV ? "KV" : "NOT_CONFIGURED",
        officialPolling: "requires deployed cron trigger"
      });
    }

    if (request.method === "GET" && url.pathname === "/results/latest") {
      if (!env.RESULTS_KV) return json({ ok: false, error: "storage_not_configured" }, 503);
      const game = url.searchParams.get("game")?.trim().toLowerCase();
      const result = await readResult(env, game ? `latest:${game}` : "latest:any");
      return result ? json(result) : json({ ok: true, result: null });
    }

    if (request.method === "POST" && url.pathname === "/refresh") {
      if (!env.PUBLISH_SECRET || request.headers.get("x-publish-secret") !== env.PUBLISH_SECRET) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }
      return json({ ok: true, checks: await refreshOfficialResults(env) });
    }

    if (request.method === "POST" && url.pathname === "/publish") {
      if (!env.PUBLISH_SECRET || request.headers.get("x-publish-secret") !== env.PUBLISH_SECRET) {
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
      endpoints: [
        "GET /health",
        "GET /results/latest?game=euromilhoes",
        "POST /refresh",
        "POST /publish"
      ]
    });
  },

  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(refreshOfficialResults(env));
  }
};

// Export the pure parser for deterministic tests without running a Worker.
export { extractOfficialResult, OFFICIAL_SOURCES };
