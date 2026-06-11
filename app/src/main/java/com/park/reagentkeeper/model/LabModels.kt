package com.park.reagentkeeper.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

enum class LabItemType(val label: String) {
    REAGENT("시약"),
    SUPPLY("소모품"),
    EQUIPMENT("기자재"),
}

enum class HazardLevel(val label: String) {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
}

enum class InventoryEventType(val label: String, val signedMultiplier: Double) {
    STOCK_IN("입고", 1.0),
    STOCK_OUT("사용", -1.0),
    AUDIT("점검", 0.0),
}

data class InventoryEvent(
    val id: String,
    val type: InventoryEventType,
    val amount: Double,
    val memo: String,
    val happenedAt: Long,
)

data class LabItem(
    val id: String,
    val name: String,
    val type: LabItemType,
    val category: String,
    val spec: String,
    val unit: String,
    val currentQuantity: Double,
    val minimumQuantity: Double,
    val storageLocation: String,
    val manager: String,
    val hazardLevel: HazardLevel,
    val expiryDate: String,
    val note: String,
    val lastCheckedAt: Long,
    val history: List<InventoryEvent>,
)

data class LabItemDraft(
    val id: String? = null,
    val name: String,
    val type: LabItemType,
    val category: String,
    val spec: String,
    val unit: String,
    val currentQuantity: Double,
    val minimumQuantity: Double,
    val storageLocation: String,
    val manager: String,
    val hazardLevel: HazardLevel,
    val expiryDate: String,
    val note: String,
)

enum class ExpiryState {
    NONE,
    NORMAL,
    SOON,
    EXPIRED,
}

fun LabItem.isLowStock(): Boolean = currentQuantity <= minimumQuantity

fun LabItem.expiryState(today: LocalDate = LocalDate.now()): ExpiryState {
    if (expiryDate.isBlank()) return ExpiryState.NONE
    val parsed = runCatching { LocalDate.parse(expiryDate) }.getOrNull() ?: return ExpiryState.NONE
    return when {
        parsed.isBefore(today) -> ExpiryState.EXPIRED
        parsed.isBefore(today.plusDays(31)) || parsed.isEqual(today.plusDays(31)) -> ExpiryState.SOON
        else -> ExpiryState.NORMAL
    }
}

fun String.isIsoDateOrBlank(): Boolean {
    if (isBlank()) return true
    return try {
        LocalDate.parse(this)
        true
    } catch (_: DateTimeParseException) {
        false
    }
}

fun Double.displayQuantity(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format("%.1f", this)
    }
}
