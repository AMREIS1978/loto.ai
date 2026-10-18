# LotoSMS — Android / Play Store

Base funcional Android (Kotlin + Jetpack Compose) para uma app de mensagens que pode enviar, pelo SIM do próprio telefone, mensagens escolhidas pelo utilizador e automatismos associados a novos resultados.

## O que já está incluído
- Pedido do papel de **app SMS predefinida** através de `ROLE_SMS`.
- Ecrã inicial moderno com navegação: Início, Jogos, Grupos, Histórico e Definições.
- Grupos de destinatários guardados localmente.
- Envio imediato para um grupo, com números duplicados eliminados.
- Motor de SMS que só envia automaticamente se a app for realmente o gestor SMS predefinido e tiver `SEND_SMS` concedido.
- Proteção contra o mesmo `drawId` ser enviado duas vezes.
- `FirebaseMessagingService` preparado para receber um novo resultado do backend e disparar o fluxo local.
- Componentes exigidos para uma app SMS: `SMS_DELIVER`, `WAP_PUSH_DELIVER` e atividade de `SENDTO`.
- Backend de referência em `backend/` para publicar um novo resultado via Firebase Cloud Messaging.

## Importante
Esta é uma base de desenvolvimento. Antes de submeter à Google Play é obrigatório terminar a experiência de gestor SMS (caixa de entrada/conversas, receção, persistência, etc.) e rever a política de permissões vigente no momento da submissão.

## Compilar
1. Abrir a pasta no Android Studio.
2. Instalar Android SDK 35 se solicitado.
3. Adicionar `google-services.json` ao módulo `app/` para usar Firebase Messaging.
4. Adicionar o plugin Google Services ao projeto antes de ativar FCM em produção.
5. Sincronizar Gradle.
6. Executar num telefone Android real com SIM para testar SMS.

Para release de Play Store, gerar um **AAB assinado** e não guardar keystore, palavras-passe ou credenciais no GitHub.
