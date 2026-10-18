# Backend de resultados — referência

Objetivo: a fonte de resultados chama este backend (ou um worker faz polling à fonte licenciada/oficial). Quando há um `drawId` novo e validado, o backend envia **apenas o resultado** aos telefones subscritos via Firebase Cloud Messaging. A lista de contactos e grupos continua no telefone.

## Fluxo recomendado
1. Worker consulta a fonte autorizada/oficial.
2. Normaliza o resultado.
3. Confirma que o `drawId` ainda não foi publicado.
4. Faz uma segunda validação da chave para evitar um envio errado.
5. Publica `{drawId, game, title, message}` num tópico FCM por jogo.
6. Cada Android recebe, aplica as preferências locais e envia SMS pelo SIM se a app for o gestor SMS predefinido.

## Segurança
- `FIREBASE_SERVICE_ACCOUNT` apenas no servidor/secret manager.
- Endpoint `/publish` protegido por `PUBLISH_SECRET`.
- HTTPS obrigatório em produção.
- Idempotência por `drawId` no backend + no Android.
- Rate limit no endpoint de publicação.
- Guardar auditoria dos resultados publicados, nunca a agenda de contactos.

## Executar
```bash
npm install
FIREBASE_SERVICE_ACCOUNT='{"project_id":"..."}' PUBLISH_SECRET='...' npm start
```
