import express from 'express';
import admin from 'firebase-admin';

const app = express();
app.use(express.json({ limit: '64kb' }));

const secret = process.env.PUBLISH_SECRET;
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT || '{}');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

// Produção: substituir por BD persistente (Postgres/Firestore) com unique(drawId, game).
const published = new Set();

app.get('/health', (_, res) => res.json({ ok: true }));

app.post('/publish', async (req, res) => {
  if (!secret || req.get('x-publish-secret') !== secret) return res.status(401).json({ error: 'unauthorized' });
  const { drawId, game, title, message } = req.body || {};
  if (![drawId, game, title, message].every(v => typeof v === 'string' && v.trim())) {
    return res.status(400).json({ error: 'invalid_payload' });
  }
  const idempotencyKey = `${game}:${drawId}`;
  if (published.has(idempotencyKey)) return res.json({ ok: true, duplicate: true });

  await admin.messaging().send({
    topic: `results-${game.toLowerCase().replace(/[^a-z0-9_-]/g, '-')}`,
    data: { drawId, game, title, message }
  });
  published.add(idempotencyKey);
  res.json({ ok: true });
});

app.listen(process.env.PORT || 8080, () => console.log('LotoSMS results API ready'));
