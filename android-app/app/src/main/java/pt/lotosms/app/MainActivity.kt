package pt.lotosms.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val sendNow = intent?.getBooleanExtra("send_now", false) == true
        if (sendNow) {
            val message = intent?.getStringExtra("message").orEmpty()
            val recipients = intent?.getStringArrayListExtra("recipients").orEmpty()
            if (message.isNotBlank() && recipients.isNotEmpty()) {
                SmsEngine.sendMany(this, recipients, message)
            }
        }

        setContent { MaterialTheme { LotoSmsApp(this) } }
    }

    fun requestContacts() {
        contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotoSmsApp(activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Início", "Jogos", "Grupos", "Histórico", "Definições")

    Scaffold(
        topBar = { TopAppBar(title = { Text("LotoSMS") }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text((index + 1).toString()) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(activity)
                1 -> GamesScreen()
                2 -> GroupsScreen(activity)
                3 -> HistoryScreen()
                else -> SettingsScreen(activity)
            }
        }
    }
}

@Composable
fun HomeScreen(context: Context) {
    val store = remember { AppStore(context) }
    var auto by remember { mutableStateOf(store.isAutomaticEnabled()) }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Resultados e mensagens", style = MaterialTheme.typography.headlineMedium)
        Card { Column(Modifier.padding(18.dp)) {
            Text("Deteção automática", style = MaterialTheme.typography.titleLarge)
            Text("A API deteta a nova chave, prepara tudo e mostra apenas: ‘Chave disponível — Enviar?’")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = auto, onCheckedChange = { auto = it; store.setAutomaticEnabled(it) })
                Spacer(Modifier.width(8.dp)); Text(if (auto) "Ligado" else "Desligado")
            }
        }}
        Card { Column(Modifier.padding(18.dp)) {
            Text("Contactos", style = MaterialTheme.typography.titleLarge)
            Text("Pode autorizar o acesso aos contactos para criar grupos rapidamente.")
            if (!ContactHelper.hasPermission(context) && context is MainActivity) {
                Button(onClick = { context.requestContacts() }) { Text("Permitir contactos") }
            } else {
                Text("Acesso aos contactos autorizado")
            }
        }}
    }
}

@Composable
fun GamesScreen() {
    val games = listOf("Euromilhões", "Totoloto", "Joker", "EuroDreams")
    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Jogos", style = MaterialTheme.typography.headlineMedium) }
        items(games) { game -> Card { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(game); Switch(checked = true, onCheckedChange = {}) } } }
    }
}

@Composable
fun GroupsScreen(context: Context) {
    val store = remember { AppStore(context) }
    var groups by remember { mutableStateOf(store.loadGroups()) }
    var name by remember { mutableStateOf("") }
    var numbers by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Boa sorte! 🍀") }

    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Grupos", style = MaterialTheme.typography.headlineMedium) }
        item {
            if (ContactHelper.hasPermission(context)) {
                Button(onClick = {
                    val contacts = ContactHelper.loadPhoneContacts(context)
                    if (contacts.isNotEmpty()) {
                        val imported = SmsGroup("contactos", "Contactos", contacts.map { it.number }, true)
                        groups = groups.filterNot { it.id == "contactos" } + imported
                        store.saveGroups(groups)
                        store.setSelectedGroupIds(groups.map { it.id }.toSet())
                    }
                }) { Text("Importar contactos") }
            }
        }
        items(groups) { group ->
            Card { Column(Modifier.padding(16.dp)) {
                Text(group.name, style = MaterialTheme.typography.titleLarge)
                Text("${group.numbers.size} destinatários")
                OutlinedButton(onClick = {
                    val result = SmsEngine.sendMany(context, group.numbers.distinct(), message)
                    store.addHistory(HistoryEntry(System.currentTimeMillis(), "Mensagem preparada — ${group.name}", group.numbers.distinct().size, result.success, result.failed))
                }) { Text("Preparar e enviar") }
            }}
        }
        item {
            HorizontalDivider()
            Text("Novo grupo", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = numbers, onValueChange = { numbers = it }, label = { Text("Números separados por vírgulas") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Mensagem rápida") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                if (name.isNotBlank()) {
                    val next = groups + SmsGroup(System.currentTimeMillis().toString(), name.trim(), numbers.split(",").map { it.trim() }.filter { it.isNotEmpty() }, true)
                    groups = next
                    store.saveGroups(next)
                    store.setSelectedGroupIds(next.map { it.id }.toSet())
                    name = ""
                    numbers = ""
                }
            }) { Text("Criar grupo") }
        }
    }
}

@Composable
fun HistoryScreen() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Histórico", style = MaterialTheme.typography.headlineMedium)
        Text("O histórico técnico é guardado localmente.")
    }
}

@Composable
fun SettingsScreen(activity: MainActivity) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Definições", style = MaterialTheme.typography.headlineMedium)
        Card { Column(Modifier.padding(16.dp)) {
            Text("Envio por SMS", style = MaterialTheme.typography.titleLarge)
            Text("A app prepara automaticamente a mensagem e os destinatários. Não precisa ser a app SMS predefinida.")
        }}
        if (!ContactHelper.hasPermission(activity)) {
            Button(onClick = { activity.requestContacts() }) { Text("Autorizar contactos") }
        }
        Text("Podem aplicar-se custos do seu tarifário móvel. Só deve adicionar destinatários que tenham consentido receber estas mensagens.")
    }
}
