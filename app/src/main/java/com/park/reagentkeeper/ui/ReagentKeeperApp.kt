package com.park.reagentkeeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.park.reagentkeeper.model.AdminStats
import com.park.reagentkeeper.model.AppUser
import com.park.reagentkeeper.model.ExpiryState
import com.park.reagentkeeper.model.HazardLevel
import com.park.reagentkeeper.model.InventoryEvent
import com.park.reagentkeeper.model.InventoryEventType
import com.park.reagentkeeper.model.LabItem
import com.park.reagentkeeper.model.LabItemDraft
import com.park.reagentkeeper.model.LabItemType
import com.park.reagentkeeper.model.LabSummary
import com.park.reagentkeeper.model.UserRole
import com.park.reagentkeeper.model.displayQuantity
import com.park.reagentkeeper.model.expiryState
import com.park.reagentkeeper.model.isIsoDateOrBlank
import com.park.reagentkeeper.model.isLowStock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class AppSection(val label: String) {
    INVENTORY("재고 관리"),
    ADMIN("관리자"),
}

private enum class InventoryFilter(val label: String) {
    ALL("전체"),
    ALERT("주의 필요"),
    REAGENT("시약"),
    SUPPLY("소모품"),
    EQUIPMENT("기자재"),
}

private data class RecentEventEntry(
    val item: LabItem,
    val event: InventoryEvent,
)

@Composable
fun ReagentKeeperApp(viewModel: InventoryViewModel = viewModel()) {
    val currentUser = viewModel.currentUser

    if (currentUser == null) {
        LoginScreen(
            errorMessage = viewModel.loginError,
            onClearError = viewModel::clearLoginError,
            onLogin = viewModel::login,
        )
        return
    }

    AuthenticatedApp(
        viewModel = viewModel,
        currentUser = currentUser,
    )
}

@Composable
private fun AuthenticatedApp(
    viewModel: InventoryViewModel,
    currentUser: AppUser,
) {
    val items = viewModel.items
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(InventoryFilter.ALL.name) }
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.INVENTORY.name) }
    var showEditor by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<LabItem?>(null) }
    var actionTarget by remember { mutableStateOf<LabItem?>(null) }
    var actionType by remember { mutableStateOf<InventoryEventType?>(null) }

    val filter = InventoryFilter.valueOf(selectedFilter)
    val section = AppSection.valueOf(selectedSection)
    val showAdmin = currentUser.role == UserRole.ADMIN && section == AppSection.ADMIN
    val filteredItems = remember(items, query, filter) { applyFilters(items, query, filter) }
    val recentEvents = remember(items) {
        items
            .flatMap { item -> item.history.map { event -> RecentEventEntry(item, event) } }
            .sortedByDescending { it.event.happenedAt }
            .take(8)
    }
    val lowStockCount = items.count { it.isLowStock() }
    val expiringCount = items.count {
        val state = it.expiryState()
        state == ExpiryState.SOON || state == ExpiryState.EXPIRED
    }
    val highHazardCount = items.count { it.hazardLevel == HazardLevel.HIGH }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (!showAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editorTarget = null
                        showEditor = true
                    },
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text("새 항목 추가")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = innerPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 108.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    AppHeader(
                        user = currentUser,
                        onLogout = {
                            selectedSection = AppSection.INVENTORY.name
                            viewModel.logout()
                        },
                    )
                }

                if (currentUser.role == UserRole.ADMIN) {
                    item {
                        SectionSwitch(
                            selectedSection = section,
                            onSelected = { selectedSection = it.name },
                        )
                    }
                }

                if (showAdmin) {
                    item {
                        AdminDashboardScreen(
                            stats = viewModel.adminStats(),
                            users = viewModel.users,
                        )
                    }
                } else {
                    item {
                        TodayPanel(
                            user = currentUser,
                            totalCount = items.size,
                            lowStockCount = lowStockCount,
                            expiringCount = expiringCount,
                        )
                    }
                    item {
                        SummaryStrip(
                            totalCount = items.size,
                            lowStockCount = lowStockCount,
                            expiringCount = expiringCount,
                            highHazardCount = highHazardCount,
                        )
                    }
                    item {
                        QuickActionPanel(
                            lowStockCount = lowStockCount,
                            expiringCount = expiringCount,
                            highHazardCount = highHazardCount,
                        )
                    }
                    item {
                        SearchAndFilterSection(
                            query = query,
                            onQueryChange = { query = it },
                            selectedFilter = filter,
                            onFilterChange = { selectedFilter = it.name },
                        )
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            EmptyInventoryState(query = query, filter = filter)
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            InventoryCard(
                                item = item,
                                onEdit = {
                                    editorTarget = item
                                    showEditor = true
                                },
                                onStockIn = {
                                    actionTarget = item
                                    actionType = InventoryEventType.STOCK_IN
                                },
                                onStockOut = {
                                    actionTarget = item
                                    actionType = InventoryEventType.STOCK_OUT
                                },
                            )
                        }
                    }

                    item {
                        RecentActivitySection(entries = recentEvents)
                    }
                }
            }
        }
    }

    ItemEditorDialog(
        initialItem = editorTarget,
        visible = showEditor,
        onDismiss = {
            showEditor = false
            editorTarget = null
        },
        onSave = {
            viewModel.upsertItem(it)
            showEditor = false
            editorTarget = null
        },
    )

    InventoryActionDialog(
        item = actionTarget,
        actionType = actionType,
        onDismiss = {
            actionTarget = null
            actionType = null
        },
        onConfirm = { itemId, eventType, amount, memo ->
            viewModel.recordMovement(itemId, eventType, amount, memo)
            actionTarget = null
            actionType = null
        },
    )
}

@Composable
private fun LoginScreen(
    errorMessage: String?,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Boolean,
) {
    var email by rememberSaveable { mutableStateOf("admin@ontect.school") }
    var password by rememberSaveable { mutableStateOf("admin1234") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    ),
                ),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OnTect",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                StatusPill(
                    text = "무료 로컬 빌드",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "학교 과학실 재고 관리",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "계정으로 로그인하면 담당 실험실 재고와 입출고 기록을 이어서 관리할 수 있습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("이메일") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("비밀번호") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Button(
                        onClick = { onLogin(email, password) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("로그인")
                    }

                    HorizontalDivider()

                    Text(
                        text = "빠른 체험 계정",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DemoAccountChip("관리자", "admin@ontect.school", "admin1234", onPick = { pickedEmail, pickedPassword ->
                            email = pickedEmail
                            password = pickedPassword
                            onClearError()
                        })
                        DemoAccountChip("화학실", "chem@ontect.school", "chem1234", onPick = { pickedEmail, pickedPassword ->
                            email = pickedEmail
                            password = pickedPassword
                            onClearError()
                        })
                        DemoAccountChip("생명실", "bio@ontect.school", "bio1234", onPick = { pickedEmail, pickedPassword ->
                            email = pickedEmail
                            password = pickedPassword
                            onClearError()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoAccountChip(
    label: String,
    email: String,
    password: String,
    onPick: (String, String) -> Unit,
) {
    AssistChip(
        onClick = { onPick(email, password) },
        label = { Text(label) },
    )
}

@Composable
private fun AppHeader(user: AppUser, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "OnTect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${user.name} · ${user.role.label} · ${user.labName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "무료 로컬 모드",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onLogout) {
            Text("로그아웃")
        }
    }
}

@Composable
private fun SectionSwitch(selectedSection: AppSection, onSelected: (AppSection) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppSection.entries.forEach { section ->
            FilterChip(
                selected = selectedSection == section,
                onClick = { onSelected(section) },
                label = { Text(section.label) },
            )
        }
    }
}

@Composable
private fun TodayPanel(
    user: AppUser,
    totalCount: Int,
    lowStockCount: Int,
    expiringCount: Int,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${user.labName} 오늘의 점검",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "부족 재고와 만료 임박 항목을 먼저 확인하고, 수업 사용량은 바로 기록하세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroPill("${totalCount}개 항목")
                    HeroPill("부족 ${lowStockCount}개")
                    HeroPill("만료주의 ${expiringCount}개")
                }
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SummaryStrip(totalCount: Int, lowStockCount: Int, expiringCount: Int, highHazardCount: Int) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SummaryCard(title = "총 재고", value = totalCount.toString(), caption = "실험실 전체")
        }
        item {
            SummaryCard(title = "부족 항목", value = lowStockCount.toString(), caption = "최소 재고 이하")
        }
        item {
            SummaryCard(title = "만료 경고", value = expiringCount.toString(), caption = "31일 이내 포함")
        }
        item {
            SummaryCard(title = "고위험", value = highHazardCount.toString(), caption = "취급 주의 필요")
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, caption: String) {
    ElevatedCard(
        modifier = Modifier.width(164.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionPanel(lowStockCount: Int, expiringCount: Int, highHazardCount: Int) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "우선 확인", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip("재주문 $lowStockCount")
                ActionChip("만료 임박 $expiringCount")
                ActionChip("고위험 $highHazardCount")
                ActionChip("수업 사용량 기록")
            }
        }
    }
}

@Composable
private fun ActionChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SearchAndFilterSection(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: InventoryFilter,
    onFilterChange: (InventoryFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            label = { Text("검색") },
            placeholder = { Text("시약명, 분류, 위치, 담당교사") },
            singleLine = true,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InventoryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryCard(
    item: LabItem,
    onEdit: () -> Unit,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
) {
    val expiryState = item.expiryState()
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                item.isLowStock() -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
                expiryState == ExpiryState.EXPIRED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f)
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(text = item.type.label, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        if (item.hazardLevel == HazardLevel.HIGH) {
                            StatusPill(text = "고위험", color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Text(text = item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = buildString {
                            append(item.category)
                            if (item.spec.isNotBlank()) append(" · ${item.spec}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(text = item.currentQuantity.displayQuantity(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = "${item.unit} 보유", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "최소 ${item.minimumQuantity.displayQuantity()} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.isLowStock()) {
                    AssistChip(onClick = {}, label = { Text("재주문 필요") })
                }
                when (expiryState) {
                    ExpiryState.SOON -> AssistChip(onClick = {}, label = { Text("유통기한 임박") })
                    ExpiryState.EXPIRED -> AssistChip(onClick = {}, label = { Text("유통기한 지남") })
                    else -> Unit
                }
                if (item.note.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text("메모 있음") })
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine(label = "보관 위치", value = item.storageLocation)
                DetailLine(label = "담당", value = item.manager.ifBlank { "미지정" })
                DetailLine(label = "유통기한", value = item.expiryDate.ifBlank { "없음" })
                DetailLine(label = "마지막 점검", value = formatTimestamp(item.lastCheckedAt))
                if (item.note.isNotBlank()) {
                    DetailLine(label = "메모", value = item.note, maxLines = 3)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStockIn, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Text("입고")
                }
                OutlinedButton(onClick = onStockOut, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Text("사용")
                }
                TextButton(onClick = onEdit) {
                    Text("수정")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, contentColor: Color) {
    Surface(
        shape = CircleShape,
        color = color,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String, maxLines: Int = 1) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyInventoryState(query: String, filter: InventoryFilter) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "조건에 맞는 항목이 없습니다", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "검색어 `${query.ifBlank { "없음" }}` 와 필터 `${filter.label}` 조합에 맞는 결과가 아직 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentActivitySection(entries: List<RecentEventEntry>) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "최근 입출고 및 점검 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (entries.isEmpty()) {
                Text(text = "아직 기록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (entry.event.type) {
                                        InventoryEventType.STOCK_IN -> MaterialTheme.colorScheme.primary
                                        InventoryEventType.STOCK_OUT -> MaterialTheme.colorScheme.tertiary
                                        InventoryEventType.AUDIT -> MaterialTheme.colorScheme.secondary
                                    },
                                ),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${entry.item.name} · ${entry.event.type.label}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = entry.event.memo.ifBlank { "메모 없음" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = when (entry.event.type) {
                                    InventoryEventType.AUDIT -> "점검"
                                    else -> "${entry.event.amount.displayQuantity()} ${entry.item.unit}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = formatTimestamp(entry.event.happenedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardScreen(stats: AdminStats, users: List<AppUser>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "관리자 대시보드",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        AdminMetricStrip(stats)
        UserStatsSection(stats)
        LabSummarySection(stats.labSummaries)
        UserRosterSection(users)
    }
}

@Composable
private fun AdminMetricStrip(stats: AdminStats) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SummaryCard("전체 유저", stats.userStats.totalUsers.toString(), "활성 ${stats.userStats.activeUsers}명") }
        item { SummaryCard("전체 재고", stats.totalItems.toString(), "등록 항목") }
        item { SummaryCard("주의 재고", (stats.lowStockItems + stats.expiringItems).toString(), "부족/만료") }
        item { SummaryCard("입출고", stats.totalEvents.toString(), "누적 기록") }
    }
}

@Composable
private fun UserStatsSection(stats: AdminStats) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "사용자 통계", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            AdminStatLine("관리자", stats.userStats.adminUsers, stats.userStats.totalUsers)
            AdminStatLine("교사", stats.userStats.teacherUsers, stats.userStats.totalUsers)
            AdminStatLine("실험 보조", stats.userStats.assistantUsers, stats.userStats.totalUsers)
            HorizontalDivider()
            AdminStatLine("입고 기록", stats.stockInEvents, stats.totalEvents)
            AdminStatLine("사용 기록", stats.stockOutEvents, stats.totalEvents)
            AdminStatLine("점검 기록", stats.auditEvents, stats.totalEvents)
        }
    }
}

@Composable
private fun AdminStatLine(label: String, value: Int, total: Int) {
    val percent = if (total == 0) 0 else ((value.toFloat() / total.toFloat()) * 100).toInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(text = "$value", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(text = "$percent%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabSummarySection(summaries: List<LabSummary>) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "실험실별 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            summaries.forEachIndexed { index, summary ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = summary.labName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "담당 ${summary.ownerCount}명 · 항목 ${summary.itemCount}개",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        text = "주의 ${summary.alertCount}",
                        color = if (summary.alertCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (summary.alertCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRosterSection(users: List<AppUser>) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "전체 유저", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            users.forEachIndexed { index, user ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${user.email} · ${user.labName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "최근 로그인: ${formatTimestampOrNone(user.lastLoginAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        text = user.role.label,
                        color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun applyFilters(items: List<LabItem>, query: String, filter: InventoryFilter): List<LabItem> {
    val normalizedQuery = query.trim().lowercase()
    return items.filter { item ->
        val queryMatches = normalizedQuery.isBlank() || listOf(
            item.name,
            item.category,
            item.spec,
            item.storageLocation,
            item.manager,
            item.note,
        ).any { value -> value.lowercase().contains(normalizedQuery) }

        val filterMatches = when (filter) {
            InventoryFilter.ALL -> true
            InventoryFilter.ALERT -> item.isLowStock() ||
                item.hazardLevel == HazardLevel.HIGH ||
                item.expiryState() == ExpiryState.SOON ||
                item.expiryState() == ExpiryState.EXPIRED
            InventoryFilter.REAGENT -> item.type == LabItemType.REAGENT
            InventoryFilter.SUPPLY -> item.type == LabItemType.SUPPLY
            InventoryFilter.EQUIPMENT -> item.type == LabItemType.EQUIPMENT
        }

        queryMatches && filterMatches
    }.sortedWith(
        compareByDescending<LabItem> { it.isLowStock() }
            .thenByDescending { it.expiryState() == ExpiryState.EXPIRED }
            .thenBy { it.name.lowercase() },
    )
}

@Composable
private fun ItemEditorDialog(
    initialItem: LabItem?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (LabItemDraft) -> Unit,
) {
    if (!visible) return

    var name by remember(initialItem?.id) { mutableStateOf(initialItem?.name.orEmpty()) }
    var category by remember(initialItem?.id) { mutableStateOf(initialItem?.category.orEmpty()) }
    var spec by remember(initialItem?.id) { mutableStateOf(initialItem?.spec.orEmpty()) }
    var unit by remember(initialItem?.id) { mutableStateOf(initialItem?.unit.orEmpty()) }
    var currentQuantity by remember(initialItem?.id) { mutableStateOf(initialItem?.currentQuantity?.displayQuantity().orEmpty()) }
    var minimumQuantity by remember(initialItem?.id) { mutableStateOf(initialItem?.minimumQuantity?.displayQuantity().orEmpty()) }
    var storageLocation by remember(initialItem?.id) { mutableStateOf(initialItem?.storageLocation.orEmpty()) }
    var manager by remember(initialItem?.id) { mutableStateOf(initialItem?.manager.orEmpty()) }
    var expiryDate by remember(initialItem?.id) { mutableStateOf(initialItem?.expiryDate.orEmpty()) }
    var note by remember(initialItem?.id) { mutableStateOf(initialItem?.note.orEmpty()) }
    var selectedType by remember(initialItem?.id) { mutableStateOf(initialItem?.type ?: LabItemType.REAGENT) }
    var selectedHazard by remember(initialItem?.id) { mutableStateOf(initialItem?.hazardLevel ?: HazardLevel.MEDIUM) }
    var errorMessage by remember(initialItem?.id) { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (initialItem == null) "새 항목 추가" else "항목 정보 수정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Column(
                    modifier = Modifier
                        .height(520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EditorChoiceRow(
                        title = "항목 유형",
                        options = LabItemType.entries.map { it.label to it },
                        selected = selectedType,
                        onSelected = { selectedType = it },
                    )
                    EditorChoiceRow(
                        title = "위험도",
                        options = HazardLevel.entries.map { it.label to it },
                        selected = selectedHazard,
                        onSelected = { selectedHazard = it },
                    )
                    EditorField(value = name, onValueChange = { name = it }, label = "항목명")
                    EditorField(value = category, onValueChange = { category = it }, label = "분류")
                    EditorField(value = spec, onValueChange = { spec = it }, label = "용량 / 모델명")
                    EditorField(value = unit, onValueChange = { unit = it }, label = "단위", placeholder = "L, kg, 병, 대")
                    EditorField(value = currentQuantity, onValueChange = { currentQuantity = it }, label = "현재 수량", keyboardType = KeyboardType.Decimal)
                    EditorField(value = minimumQuantity, onValueChange = { minimumQuantity = it }, label = "최소 수량", keyboardType = KeyboardType.Decimal)
                    EditorField(value = storageLocation, onValueChange = { storageLocation = it }, label = "보관 위치")
                    EditorField(value = manager, onValueChange = { manager = it }, label = "담당 교사 / 실험실")
                    EditorField(value = expiryDate, onValueChange = { expiryDate = it }, label = "유통기한", placeholder = "YYYY-MM-DD, 없으면 비워두기")
                    EditorField(value = note, onValueChange = { note = it }, label = "메모", singleLine = false)
                }

                if (errorMessage != null) {
                    Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("취소")
                    }
                    Button(
                        onClick = {
                            val parsedCurrent = currentQuantity.toDoubleOrNull()
                            val parsedMinimum = minimumQuantity.toDoubleOrNull()
                            when {
                                name.isBlank() -> errorMessage = "항목명을 입력해 주세요."
                                category.isBlank() -> errorMessage = "분류를 입력해 주세요."
                                unit.isBlank() -> errorMessage = "단위를 입력해 주세요."
                                storageLocation.isBlank() -> errorMessage = "보관 위치를 입력해 주세요."
                                parsedCurrent == null || parsedCurrent < 0 -> errorMessage = "현재 수량은 0 이상 숫자로 입력해 주세요."
                                parsedMinimum == null || parsedMinimum < 0 -> errorMessage = "최소 수량은 0 이상 숫자로 입력해 주세요."
                                !expiryDate.isIsoDateOrBlank() -> errorMessage = "유통기한은 YYYY-MM-DD 형식으로 입력해 주세요."
                                else -> {
                                    errorMessage = null
                                    onSave(
                                        LabItemDraft(
                                            id = initialItem?.id,
                                            name = name,
                                            type = selectedType,
                                            category = category,
                                            spec = spec,
                                            unit = unit,
                                            currentQuantity = parsedCurrent,
                                            minimumQuantity = parsedMinimum,
                                            storageLocation = storageLocation,
                                            manager = manager,
                                            hazardLevel = selectedHazard,
                                            expiryDate = expiryDate,
                                            note = note,
                                        ),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun <T> EditorChoiceRow(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (label, value) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun InventoryActionDialog(
    item: LabItem?,
    actionType: InventoryEventType?,
    onDismiss: () -> Unit,
    onConfirm: (String, InventoryEventType, Double, String) -> Unit,
) {
    if (item == null || actionType == null) return

    var amountText by remember(item.id, actionType) { mutableStateOf("") }
    var memo by remember(item.id, actionType) { mutableStateOf("") }
    var errorMessage by remember(item.id, actionType) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.name} ${actionType.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("수량") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    placeholder = { Text("수업명, 구매처, 폐기 사유 등") },
                    singleLine = false,
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                )
                if (errorMessage != null) {
                    Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                } else if (actionType == InventoryEventType.STOCK_OUT) {
                    Text(
                        text = "현재 재고: ${item.currentQuantity.displayQuantity()} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> errorMessage = "0보다 큰 수량을 입력해 주세요."
                        else -> onConfirm(item.id, actionType, amount, memo)
                    }
                },
            ) {
                Text("적용")
            }
        },
    )
}

private fun formatTimestamp(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun formatTimestampOrNone(epochMillis: Long): String {
    return if (epochMillis == 0L) "기록 없음" else formatTimestamp(epochMillis)
}
