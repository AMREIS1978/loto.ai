# LOTO AI — Android

## Estado técnico verificado em 22/10/2026
- O envio SMS usa `SmsManager` com a permissão `SEND_SMS`; **o código atual não exige ser a aplicação SMS predefinida**. A documentação anterior estava incorreta.
- O envio depende de um telefone ligado, SIM ativo, rede móvel e autorização de SMS; pode ter custos da operadora.
- `ResultSyncWorker` usa WorkManager com intervalo mínimo de 15 minutos, sujeito a atrasos e restrições de bateria do Android. Não é execução contínua garantida.
- O servidor Cloudflare pode consultar resultados de cinco em cinco minutos se o cron estiver efetivamente publicado, mas exige binding persistente `RESULTS_KV`.
- A receção de notificações Firebase e o envio de SMS ainda requerem validação num telefone real.
- A compilação GitHub Actions de 20/10/2026 gerou uma APK debug; a APK de atualização assinada não foi produzida.

## Antes de considerar pronta
1. Configurar e publicar Cloudflare KV, cron e credenciais de notificações, sem expor segredos no repositório.
2. Validar os resultados reais de Euromilhões, Totoloto e EuroDreams, inclusive mudanças no HTML oficial e prevenção de duplicados.
3. Testar permissões SMS, SIM, entrega, erros e reinícios do Android em dispositivo real.
4. Configurar assinatura de release estável, compilar APK assinada e testar atualização sobre a instalação anterior.
5. Verificar regras atuais da Google Play para permissões SMS antes de qualquer publicação.

## Compilação
O projeto Android está em `android-app/`. Para testar em dispositivo, usar Android Studio/Gradle e instalar a APK de debug. Não confundir compilação bem-sucedida com validação funcional de ponta a ponta.
