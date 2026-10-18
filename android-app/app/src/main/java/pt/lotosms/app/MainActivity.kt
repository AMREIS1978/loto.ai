package pt.lotosms.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import android.provider.Telephony
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
    private val smsPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { LotoSmsApp(this) } }
    }

    fun requestSmsRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
        }
    }

    fun requestSmsPermissions() {
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) return
        smsPermissions.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
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
            Text("Envio automático", style = MaterialTheme.typography.titleLarge)
            Text("Quando a API confirmar um novo resultado, envia pelo seu SIM apenas para os grupos escolhidos.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = auto, onCheckedChange = { auto = it; store.setAutomaticEnabled(it) })
                Spacer(Modifier.width(8.dp)); Text(if (auto) "Ligado" else "Desligado")
            }
        }}
        Card { Column(Modifier.padding(18.dp)) {
            Text("Segurança", style = MaterialTheme.typography.titleLarge)
            Text("O automatismo só funciona se LotoSMS estiver definida como app SMS predefinida. A lista de contactos permanece no telefone.")
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
        items(groups) { group ->
            Card { Column(Modifier.padding(16.dp)) {
                Text(group.name, style = MaterialTheme.typography.titleLarge)
                Text("${group.numbers.size} destinatários")
                OutlinedButton(onClick = {
                    val result = SmsEngine.sendMany(context, group.numbers.distinct(), message)
                    store.addHistory(HistoryEntry(System.currentTimeMillis(), "Mensagem rápida — ${group.name}", group.numbers.distinct().size, result.success, result.failed))
                }) { Text("Enviar agora") }
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
                    groups = next; store.saveGroups(next); name = ""; numbers = ""
                }
            }) { Text("Criar grupo") }
        }
    }
}

@Composable
fun HistoryScreen() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Histórico", style = MaterialTheme.typography.headlineMedium)
        Text("O histórico técnico é guardado localmente. Na próxima versão este ecrã apresentará cada envio, estado e destinatários falhados.")
    }
}

@Composable
fun SettingsScreen(activity: MainActivity) {
    val isDefault = Telephony.Sms.getDefaultSmsPackage(activity) == activity.packageName
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Definições", style = MaterialTheme.typography.headlineMedium)
        Card { Column(Modifier.padding(16.dp)) {
            Text("App SMS predefinida", style = MaterialTheme.typography.titleLarge)
            Text(if (isDefault) "Ativa" else "Necessária para envio automático pelo seu próprio número.")
            Button(onClick = { activity.requestSmsRole() }) { Text("Definir como app SMS") }
            if (isDefault) OutlinedButton(onClick = { activity.requestSmsPermissions() }) { Text("Autorizar SMS") }
        }}
        Text("Ao ativar envios automáticos, podem aplicar-se custos do seu tarifário móvel. Só deve adicionar destinatários que tenham consentido receber estas mensagens.")
    }
}
