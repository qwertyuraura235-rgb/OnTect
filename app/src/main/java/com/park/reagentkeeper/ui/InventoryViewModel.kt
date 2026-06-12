package com.park.reagentkeeper.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.park.reagentkeeper.data.AuthStore
import com.park.reagentkeeper.data.LabInventoryStore
import com.park.reagentkeeper.model.AdminStats
import com.park.reagentkeeper.model.AppUser
import com.park.reagentkeeper.model.ExpiryState
import com.park.reagentkeeper.model.HazardLevel
import com.park.reagentkeeper.model.InventoryEvent
import com.park.reagentkeeper.model.InventoryEventType
import com.park.reagentkeeper.model.LabSummary
import com.park.reagentkeeper.model.LabItem
import com.park.reagentkeeper.model.LabItemDraft
import com.park.reagentkeeper.model.UserRole
import com.park.reagentkeeper.model.UserStats
import com.park.reagentkeeper.model.expiryState
import com.park.reagentkeeper.model.isLowStock
import java.util.UUID

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LabInventoryStore(application)
    private val authStore = AuthStore(application)

    var items by mutableStateOf(store.loadItems())
        private set

    var users by mutableStateOf(authStore.loadUsers())
        private set

    var currentUser by mutableStateOf<AppUser?>(authStore.currentUser())
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String): Boolean {
        val result = authStore.login(email, password)
        loginError = result.errorMessage
        currentUser = result.user
        users = authStore.loadUsers()
        return result.user != null
    }

    fun logout() {
        authStore.logout()
        currentUser = null
        loginError = null
    }

    fun clearLoginError() {
        loginError = null
    }

    fun adminStats(): AdminStats {
        val lowStockItems = items.count { it.isLowStock() }
        val expiringItems = items.count {
            val state = it.expiryState()
            state == ExpiryState.SOON || state == ExpiryState.EXPIRED
        }
        val highHazardItems = items.count { it.hazardLevel == HazardLevel.HIGH }
        val events = items.flatMap { it.history }
        val labSummaries = users
            .groupBy { it.labName }
            .map { (labName, labUsers) ->
                val labItems = items.filter { item ->
                    item.manager == labName || item.storageLocation.contains(labName.removeSuffix(" 담당"))
                }
                LabSummary(
                    labName = labName,
                    ownerCount = labUsers.size,
                    itemCount = labItems.size,
                    alertCount = labItems.count { item ->
                        item.isLowStock() ||
                            item.hazardLevel == HazardLevel.HIGH ||
                            item.expiryState() == ExpiryState.SOON ||
                            item.expiryState() == ExpiryState.EXPIRED
                    },
                )
            }
            .sortedWith(compareByDescending<LabSummary> { it.alertCount }.thenBy { it.labName })

        return AdminStats(
            userStats = UserStats(
                totalUsers = users.size,
                activeUsers = users.count { it.active },
                adminUsers = users.count { it.role == UserRole.ADMIN },
                teacherUsers = users.count { it.role == UserRole.TEACHER },
                assistantUsers = users.count { it.role == UserRole.ASSISTANT },
            ),
            totalItems = items.size,
            lowStockItems = lowStockItems,
            expiringItems = expiringItems,
            highHazardItems = highHazardItems,
            totalEvents = events.size,
            stockInEvents = events.count { it.type == InventoryEventType.STOCK_IN },
            stockOutEvents = events.count { it.type == InventoryEventType.STOCK_OUT },
            auditEvents = events.count { it.type == InventoryEventType.AUDIT },
            labSummaries = labSummaries,
        )
    }

    fun upsertItem(draft: LabItemDraft) {
        val now = System.currentTimeMillis()
        val existing = items.firstOrNull { it.id == draft.id }
        val normalized = LabItem(
            id = draft.id ?: UUID.randomUUID().toString(),
            name = draft.name.trim(),
            type = draft.type,
            category = draft.category.trim(),
            spec = draft.spec.trim(),
            unit = draft.unit.trim(),
            currentQuantity = draft.currentQuantity,
            minimumQuantity = draft.minimumQuantity,
            storageLocation = draft.storageLocation.trim(),
            manager = draft.manager.trim(),
            hazardLevel = draft.hazardLevel,
            expiryDate = draft.expiryDate.trim(),
            note = draft.note.trim(),
            lastCheckedAt = now,
            history = if (existing == null) {
                listOf(
                    InventoryEvent(
                        id = UUID.randomUUID().toString(),
                        type = InventoryEventType.AUDIT,
                        amount = draft.currentQuantity,
                        memo = "초기 등록",
                        happenedAt = now,
                    ),
                )
            } else {
                existing.history
            },
        )

        val updated = items
            .filterNot { it.id == normalized.id }
            .plus(normalized)
            .sortedBy { it.name.lowercase() }

        persist(updated)
    }

    fun recordMovement(itemId: String, type: InventoryEventType, amount: Double, memo: String) {
        val now = System.currentTimeMillis()
        val updated = items.map { item ->
            if (item.id != itemId) {
                item
            } else {
                val adjustedQuantity = when (type) {
                    InventoryEventType.STOCK_IN -> item.currentQuantity + amount
                    InventoryEventType.STOCK_OUT -> (item.currentQuantity - amount).coerceAtLeast(0.0)
                    InventoryEventType.AUDIT -> item.currentQuantity
                }

                item.copy(
                    currentQuantity = adjustedQuantity,
                    lastCheckedAt = now,
                    history = listOf(
                        InventoryEvent(
                            id = UUID.randomUUID().toString(),
                            type = type,
                            amount = amount,
                            memo = memo.trim(),
                            happenedAt = now,
                        ),
                    ) + item.history,
                )
            }
        }

        persist(updated.sortedBy { it.name.lowercase() })
    }

    private fun persist(updated: List<LabItem>) {
        items = updated
        store.saveItems(updated)
    }
}
