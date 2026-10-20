import assert from "node:assert/strict";
import test from "node:test";
import { extractOfficialResult, OFFICIAL_SOURCES } from "./src/index.js";

const examples = [
  ["euromilhoes", "7 15 16 22 35 + 4 6", "Estrelas: 4 - 6"],
  ["totoloto", "1 12 20 30 45 + 7", "N.º da Sorte: 7"],
  ["eurodreams", "1 2 10 15 23 26 + 3", "N.º de Sonho: 3"]
];

for (const [game, key, extra] of examples) {
  test(game + " correctly formats a draw SMS", () => {
    const source = OFFICIAL_SOURCES.find(s => s.game === game);
    const html = "<div>Sorteio: 084/2026 Data: 20/10/2026 Chave " + key + "</div>";
    const result = extractOfficialResult(source, html);
    assert.ok(result, "parse must succeed");
    assert.equal(result.drawId, game + ":084/2026");
    assert.ok(result.message.includes("Data: 20/10/2026"));
    assert.ok(result.message.includes("Sorteio n.º 084/2026"));
    assert.ok(result.message.includes(extra));
    assert.ok(result.message.includes("\n"), "must contain actual line breaks");
    assert.ok(!result.message.includes("\\n"), "must not contain literal slash-n");
  });
}
test("rejects duplicate numbers", () => {
  const source = OFFICIAL_SOURCES[0];
  assert.equal(extractOfficialResult(source, "Sorteio: 084/2026 Chave 1 1 3 4 5 + 6 7"), null);
});
test("rejects out-of-range numbers", () => {
  const source = OFFICIAL_SOURCES[0];
  assert.equal(extractOfficialResult(source, "Sorteio: 084/2026 Chave 1 2 3 4 51 + 6 7"), null);
});
