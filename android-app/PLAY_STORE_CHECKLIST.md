# Play Store checklist — LotoSMS

## Requisitos de produto antes de submeter
- Implementar uma caixa de entrada/conversas SMS completa (a app pede ROLE_SMS e tem de ser realmente utilizável como gestor SMS).
- Explicar claramente, antes do pedido ROLE_SMS, por que razão a app precisa de ser a app SMS predefinida.
- Mostrar consentimento separado para “envio automático” e explicar que podem existir custos da operadora.
- Permitir desligar automatismos globalmente e por grupo/jogo.
- Implementar histórico com estado enviado/erro e botão para repetir apenas falhados.
- Implementar receção SMS e persistência robusta, incluindo threads/conversas.
- Testar Dual SIM e permitir escolher SIM preferido.
- Adicionar suporte a STOP/opt-out quando a app for usada para distribuição recorrente a terceiros.

## Dados e privacidade
- Contactos ficam localmente no dispositivo.
- Backend recebe token FCM e preferências mínimas, nunca a agenda completa.
- Não colocar chaves privadas no APK.
- Publicar Política de Privacidade em URL pública e dentro da app.
- Preencher Data Safety com rigor.

## Release
- Usar applicationId definitivo.
- Assinatura release com Play App Signing.
- Gerar AAB (Android App Bundle), não apenas APK.
- Teste fechado antes de produção.
