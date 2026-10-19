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
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val smsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (!SmsEngine.canSend(this)) smsPermission.launch(Manifest.permission.SEND_SMS)
        setContent { MaterialTheme { LotoSmsApp(this) } }
    }

    fun requestContacts() = contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    fun requestSms() = smsPermission.launch(Manifest.permission.SEND_SMS)
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
                1 -> GamesScreen(activity)
                2 -> GroupsScreen(activity)
                3 -> BulkSendScreen(activity)
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
        Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resultados automáticos", style = MaterialTheme.typography.titleLarge)
            Text("Quando sair uma nova chave de um jogo selecionado, a aplicação processa-a uma única vez e envia-a aos grupos escolhidos.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = auto, onCheckedChange = { auto = it; store.setAutomaticEnabled(it) })
                Spacer(Modifier.width(8.dp)); Text(if (auto) "Ligado" else "Desligado")
            }
            if (!SmsEngine.canSend(context) && context is MainActivity) {
                Button(onClick = { context.requestSms() }) { Text("Permitir envio de SMS") }
            } else {
                Text("Envio SMS autorizado", style = MaterialTheme.typography.bodySmall)
            }
        }}
        Card { Column(Modifier.padding(18.dp)) {
            Text("Contactos", style = MaterialTheme.typography.titleLarge)
            Text("Autorize os contactos para os adicionar diretamente aos seus grupos.")
            if (!ContactHelper.hasPermission(context) && context is MainActivity) {
                Button(onClick = { context.requestContacts() }) { Text("Permitir contactos") }
            } else Text("Contactos disponíveis")
        }}
    }
}

data class LotteryGame(
    val name: String,
    val days: Set<DayOfWeek>,
    val hour: Int = 20,
    val minute: Int = 0
)

private val lisbonZone: ZoneId = ZoneId.of("Europe/Lisbon")

private val lotteryGames = listOf(
    LotteryGame("Euromilhões", setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)),
    LotteryGame("Totoloto", setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)),
    LotteryGame("Joker", setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)),
    LotteryGame("EuroDreams", setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
)

private fun nextDraw(game: LotteryGame, now: ZonedDateTime): ZonedDateTime {
    for (offset in 0..7) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        if (date.dayOfWeek in game.days) {
            val candidate = date.atTime(game.hour, game.minute).atZone(lisbonZone)
            if (candidate.isAfter(now)) return candidate
        }
    }
    return now.plusDays(7)
}

private fun countdownText(now: ZonedDateTime, target: ZonedDateTime): String {
    val seconds = Duration.between(now, target).seconds.coerceAtLeast(0)
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60
    val secs = seconds % 60
    return if (days > 0) {
        "%dd %02dh %02dm %02ds".format(days, hours, minutes, secs)
    } else {
        "%02dh %02dm %02ds".format(hours, minutes, secs)
    }
}

@Composable
fun GamesScreen(context: Context) {
    val store = remember { AppStore(context) }
    var selectedGames by remember { mutableStateOf(store.selectedGames()) }
    var now by remember { mutableStateOf(ZonedDateTime.now(lisbonZone)) }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(lisbonZone)
            delay(1_000)
        }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, dd MMM · HH:mm", Locale("pt", "PT"))
    }

    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Jogos", style = MaterialTheme.typography.headlineMedium)
            Text("Escolha os sorteios a acompanhar. A contagem atualiza em tempo real.")
        }
        items(lotteryGames, key = { it.name }) { game ->
            val enabled = selectedGames.contains(game.name)
            val draw = nextDraw(game, now)
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(game.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (enabled) "Próximo sorteio: ${dateFormatter.format(draw)}" else "Sorteio desativado",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                selectedGames = if (checked) selectedGames + game.name else selectedGames - game.name
                                store.setSelectedGames(selectedGames)
                            }
                        )
                    }
                    if (enabled) {
                        Text("Faltam", style = MaterialTheme.typography.labelLarge)
                        Text(countdownText(now, draw), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsScreen(activity: MainActivity) {
    val store = remember { AppStore(activity) }
    var groups by remember { mutableStateOf(store.loadGroups()) }
    var editing by remember { mutableStateOf<SmsGroup?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Grupos", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { creating = true }) { Text("Novo grupo") }
            }
        }
        if (!ContactHelper.hasPermission(activity)) {
            item { OutlinedButton(onClick = { activity.requestContacts() }) { Text("Dar acesso aos contactos") } }
        }
        items(groups, key = { it.id }) { group ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(group.name, style = MaterialTheme.typography.titleLarge); Text("${group.numbers.size} contactos") }
                        Switch(
                            checked = group.enabled,
                            onCheckedChange = { enabled ->
                                groups = groups.map { if (it.id == group.id) it.copy(enabled = enabled) else it }
                                store.saveGroups(groups)
                                store.setSelectedGroupIds(groups.filter { it.enabled }.map { it.id }.toSet())
                            }
                        )
                    }
                    OutlinedButton(onClick = { editing = group }) { Text("Editar grupo") }
                }
            }
        }
    }

    if (creating) {
        GroupEditorDialog(
            activity = activity,
            initial = null,
            onDismiss = { creating = false },
            onSave = { newGroup ->
                groups = groups + newGroup
                store.saveGroups(groups)
                store.setSelectedGroupIds(groups.filter { it.enabled }.map { it.id }.toSet())
                creating = false
            },
            onDelete = null
        )
    }

    editing?.let { group ->
        GroupEditorDialog(
            activity = activity,
            initial = group,
            onDismiss = { editing = null },
            onSave = { updated ->
                groups = groups.map { if (it.id == updated.id) updated else it }
                store.saveGroups(groups)
                store.setSelectedGroupIds(groups.filter { it.enabled }.map { it.id }.toSet())
                editing = null
            },
            onDelete = {
                groups = groups.filterNot { it.id == group.id }
                store.saveGroups(groups)
                store.setSelectedGroupIds(groups.filter { it.enabled }.map { it.id }.toSet())
                editing = null
            }
        )
    }
}

@Composable
private fun GroupEditorDialog(
    activity: MainActivity,
    initial: SmsGroup?,
    onDismiss: () -> Unit,
    onSave: (SmsGroup) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var manualNumbers by remember(initial?.id) { mutableStateOf(initial?.numbers?.joinToString(", ").orEmpty()) }
    var search by remember { mutableStateOf("") }
    val contacts = remember(initial?.id, ContactHelper.hasPermission(activity)) {
        if (ContactHelper.hasPermission(activity)) ContactHelper.loadPhoneContacts(activity) else emptyList()
    }
    val selected = remember(initial?.id) {
        mutableStateMapOf<String, Boolean>().apply { initial?.numbers?.forEach { put(normalizePhone(it), true) } }
    }
    val visible = contacts.filter { search.isBlank() || it.name.contains(search, true) || it.number.contains(search, true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo grupo" else "Editar grupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 520.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome do grupo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    manualNumbers,
                    { manualNumbers = it },
                    label = { Text("Números manuais") },
                    supportingText = { Text("Separe por vírgulas") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!ContactHelper.hasPermission(activity)) {
                    Button(onClick = { activity.requestContacts() }) { Text("Aceder aos contactos") }
                } else {
                    OutlinedTextField(search, { search = it }, label = { Text("Pesquisar contactos") }, modifier = Modifier.fillMaxWidth())
                    LazyColumn(Modifier.heightIn(max = 240.dp)) {
                        items(visible, key = { it.number + it.name }) { contact ->
                            val key = normalizePhone(contact.number)
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = selected[key] == true, onCheckedChange = { selected[key] = it })
                                Column { Text(contact.name); Text(contact.number, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val manual = manualNumbers.split(",", ";", "\n").map { it.trim() }.filter { it.isNotBlank() }
                val fromContacts = contacts.filter { selected[normalizePhone(it.number)] == true }.map { it.number }
                val numbers = (manual + fromContacts).map { it.trim() }.filter { it.isNotBlank() }.distinctBy { normalizePhone(it) }
                if (name.isNotBlank()) {
                    onSave(SmsGroup(initial?.id ?: System.currentTimeMillis().toString(), name.trim(), numbers, initial?.enabled ?: true))
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Apagar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

private fun normalizePhone(value: String): String = value.filter { it.isDigit() || it == '+' }

@Composable
fun BulkSendScreen(context: Context) {
    val store = remember { AppStore(context) }
    val groups = remember { store.loadGroups() }
    var message by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var status by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("SMS") }

    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Envio em massa", style = MaterialTheme.typography.headlineMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = channel == "SMS", onClick = { channel = "SMS" }, label = { Text("SMS") })
                FilterChip(selected = channel == "WhatsApp", onClick = { channel = "WhatsApp" }, label = { Text("WhatsApp") })
            }
        }
        item { OutlinedTextField(message, { message = it }, label = { Text("Mensagem") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
        items(groups, key = { it.id }) { group ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selected[group.id] == true, onCheckedChange = { selected[group.id] = it })
                Text("${group.name} · ${group.numbers.size} contactos")
            }
        }
        item {
            val recipients = groups.filter { selected[it.id] == true }.flatMap { it.numbers }.distinctBy { normalizePhone(it) }
            Text("${recipients.size} destinatários únicos")
            Button(enabled = message.isNotBlank() && (channel == "WhatsApp" || recipients.isNotEmpty()), onClick = {
                if (channel == "SMS") {
                    if (!SmsEngine.canSend(context) && context is MainActivity) {
                        context.requestSms()
                        status = "Autorize o envio de SMS e volte a tocar em Enviar."
                    } else {
                        val result = SmsEngine.sendMany(context, recipients, message)
                        store.addHistory(HistoryEntry(System.currentTimeMillis(), "Envio em massa SMS", recipients.size, result.success, result.failed))
                        status = "${result.success} enviados · ${result.failed} falharam"
                    }
                } else {
                    val ok = WhatsAppEngine.shareMessage(context, message)
                    store.addHistory(HistoryEntry(System.currentTimeMillis(), "Envio WhatsApp", recipients.size, if (ok) 1 else 0, if (ok) 0 else 1))
                    status = if (ok) "WhatsApp aberto com a mensagem preparada" else "Não foi possível abrir o WhatsApp"
                }
            }) { Text(if (channel == "SMS") "Enviar SMS aos grupos" else "Abrir WhatsApp") }
            if (channel == "WhatsApp") {
                Text("A mensagem fica pronta no WhatsApp; escolha o contacto ou grupo e confirme o envio.", style = MaterialTheme.typography.bodySmall)
            }
            if (status.isNotBlank()) Text(status)
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
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resultados automáticos", style = MaterialTheme.typography.titleLarge)
            Text("A app sincroniza novos resultados em segundo plano e evita repetir o mesmo sorteio.")
            Text(if (SmsEngine.canSend(activity)) "SMS autorizado" else "Falta autorizar o envio de SMS")
            if (!SmsEngine.canSend(activity)) Button(onClick = { activity.requestSms() }) { Text("Autorizar SMS") }
        }}
        Card { Column(Modifier.padding(16.dp)) {
            Text("WhatsApp", style = MaterialTheme.typography.titleLarge)
            Text("A app prepara a mensagem e abre diretamente o WhatsApp normal ou Business que estiver instalado.")
        }}
        if (!ContactHelper.hasPermission(activity)) Button(onClick = { activity.requestContacts() }) { Text("Autorizar contactos") }
        Text("Podem aplicar-se custos do seu tarifário móvel. Utilize apenas destinatários que tenham consentido receber estas mensagens.")
    }
}
