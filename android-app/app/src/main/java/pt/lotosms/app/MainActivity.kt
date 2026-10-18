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
    val tabs = listOf("Início", "Jogos", "Grupos", "Em massa", "Histórico", "Definições")

    Scaffold(
        topBar = { TopAppBar(title = { Text("LotoSMS") }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text((index + 1).toString()) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(activity)
                1 -> GamesScreen()
                2 -> GroupsScreen(activity)
                3 -> BulkScreen(activity)
                4 -> HistoryScreen()
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
            Text("Resultados automáticos", style = MaterialTheme.typography.titleLarge)
            Text("A API deteta uma nova chave e associa automaticamente a mensagem aos grupos selecionados.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = auto, onCheckedChange = { auto = it; store.setAutomaticEnabled(it) })
                Spacer(Modifier.width(8.dp)); Text(if (auto) "Ligado" else "Desligado")
            }
        }}
        Card { Column(Modifier.padding(18.dp)) {
            Text("Contactos", style = MaterialTheme.typography.titleLarge)
            Text("Autorize o acesso aos contactos para criar e gerir grupos rapidamente.")
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
        items(games) { game ->
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(game)
                    Switch(checked = true, onCheckedChange = {})
                }
            }
        }
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
                    val unique = group.numbers.distinct()
                    val result = SmsEngine.sendMany(context, unique, message)
                    store.addHistory(
                        HistoryEntry(
                            System.currentTimeMillis(),
                            "Mensagem preparada — ${group.name}",
                            unique.size,
                            result.success,
                            result.failed
                        )
                    )
                }) { Text("Enviar a este grupo") }
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
                    val next = groups + SmsGroup(
                        System.currentTimeMillis().toString(),
                        name.trim(),
                        numbers.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        true
                    )
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
fun BulkScreen(context: Context) {
    val store = remember { AppStore(context) }
    val groups = remember { store.loadGroups() }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var message by remember { mutableStateOf("") }

    val recipients = groups
        .filter { selected.contains(it.id) }
        .flatMap { it.numbers }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Envio em massa", style = MaterialTheme.typography.headlineMedium)
            Text("Selecione um ou vários grupos. Números repetidos são removidos automaticamente.")
        }
        items(groups) { group ->
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected.contains(group.id),
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + group.id else selected - group.id
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(group.name, style = MaterialTheme.typography.titleMedium)
                        Text("${group.numbers.size} contactos")
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Mensagem") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(Modifier.height(8.dp))
            Text("${recipients.size} destinatários únicos")
            Button(
                enabled = message.isNotBlank() && recipients.isNotEmpty(),
                onClick = {
                    val result = SmsEngine.sendMany(context, recipients, message)
                    store.addHistory(
                        HistoryEntry(
                            System.currentTimeMillis(),
                            "Envio em massa",
                            recipients.size,
                            result.success,
                            result.failed
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Preparar envio em massa") }
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
            Text("Mensagens", style = MaterialTheme.typography.titleLarge)
            Text("Resultados automáticos e envio em massa usam os grupos configurados na app.")
        }}
        if (!ContactHelper.hasPermission(activity)) {
            Button(onClick = { activity.requestContacts() }) { Text("Autorizar contactos") }
        }
        Text("Podem aplicar-se custos do seu tarifário móvel. Utilize apenas contactos que tenham consentido receber estas mensagens.")
    }
}
