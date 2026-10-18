package pt.lotosms.app

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ResultPushService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val d = remoteMessage.data
        val drawId = d["drawId"] ?: return
        val game = d["game"] ?: "result"
        val title = d["title"] ?: game
        val message = d["message"] ?: return
        Automation.onNewDraw(this, DrawPayload(drawId, game, title, message))
    }

    override fun onNewToken(token: String) {
        // Produção: registar este token no backend via HTTPS autenticado.
        // Nunca guardar uma chave privada de servidor na aplicação.
    }
}
