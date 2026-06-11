package com.park.reagentkeeper.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.park.reagentkeeper.data.LabInventoryStore
import com.park.reagentkeeper.model.InventoryEvent
import com.park.reagentkeeper.model.InventoryEventType
import com.park.reagentkeeper.model.LabItem
import com.park.reagentkeeper.model.LabItemDraft
import java.util.UUID

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LabInventoryStore(application)

    var items by mutableStateOf(store.loadItems())
        private set

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
