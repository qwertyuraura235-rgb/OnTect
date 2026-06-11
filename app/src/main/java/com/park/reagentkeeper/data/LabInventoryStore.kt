package com.park.reagentkeeper.data

import android.content.Context
import com.park.reagentkeeper.model.HazardLevel
import com.park.reagentkeeper.model.InventoryEvent
import com.park.reagentkeeper.model.InventoryEventType
import com.park.reagentkeeper.model.LabItem
import com.park.reagentkeeper.model.LabItemType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class LabInventoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadItems(): List<LabItem> {
        val raw = preferences.getString(KEY_ITEMS, null)
        if (raw.isNullOrBlank()) {
            val seeded = seedItems()
            saveItems(seeded)
            return seeded
        }

        return runCatching { parseItems(JSONArray(raw)) }
            .getOrElse {
                val seeded = seedItems()
                saveItems(seeded)
                seeded
            }
    }

    fun saveItems(items: List<LabItem>) {
        val payload = JSONArray()
        items.forEach { item ->
            payload.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("type", item.type.name)
                    put("category", item.category)
                    put("spec", item.spec)
                    put("unit", item.unit)
                    put("currentQuantity", item.currentQuantity)
                    put("minimumQuantity", item.minimumQuantity)
                    put("storageLocation", item.storageLocation)
                    put("manager", item.manager)
                    put("hazardLevel", item.hazardLevel.name)
                    put("expiryDate", item.expiryDate)
                    put("note", item.note)
                    put("lastCheckedAt", item.lastCheckedAt)
                    put(
                        "history",
                        JSONArray().apply {
                            item.history.forEach { event ->
                                put(
                                    JSONObject().apply {
                                        put("id", event.id)
                                        put("type", event.type.name)
                                        put("amount", event.amount)
                                        put("memo", event.memo)
                                        put("happenedAt", event.happenedAt)
                                    },
                                )
                            }
                        },
                    )
                },
            )
        }

        preferences.edit().putString(KEY_ITEMS, payload.toString()).apply()
    }

    private fun parseItems(payload: JSONArray): List<LabItem> {
        return buildList {
            for (index in 0 until payload.length()) {
                val item = payload.getJSONObject(index)
                add(
                    LabItem(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        type = item.optEnum("type", LabItemType.REAGENT),
                        category = item.optString("category"),
                        spec = item.optString("spec"),
                        unit = item.optString("unit"),
                        currentQuantity = item.optDouble("currentQuantity", 0.0),
                        minimumQuantity = item.optDouble("minimumQuantity", 0.0),
                        storageLocation = item.optString("storageLocation"),
                        manager = item.optString("manager"),
                        hazardLevel = item.optEnum("hazardLevel", HazardLevel.LOW),
                        expiryDate = item.optString("expiryDate"),
                        note = item.optString("note"),
                        lastCheckedAt = item.optLong("lastCheckedAt", System.currentTimeMillis()),
                        history = item.optJSONArray("history").toEvents(),
                    ),
                )
            }
        }.sortedBy { it.name.lowercase() }
    }

    private fun seedItems(): List<LabItem> {
        val now = System.currentTimeMillis()
        return listOf(
            LabItem(
                id = UUID.randomUUID().toString(),
                name = "에탄올 70%",
                type = LabItemType.REAGENT,
                category = "살균/시약",
                spec = "4L",
                unit = "L",
                currentQuantity = 4.0,
                minimumQuantity = 2.0,
                storageLocation = "시약장 A-1",
                manager = "생명과학실",
                hazardLevel = HazardLevel.MEDIUM,
                expiryDate = "2026-09-30",
                note = "소독 및 일반 실험 준비용",
                lastCheckedAt = daysAgo(now, 1),
                history = listOf(
                    seededEvent(InventoryEventType.STOCK_IN, 2.0, "학기 초 보충", daysAgo(now, 14)),
                    seededEvent(InventoryEventType.STOCK_OUT, 0.5, "세포 배양 실험", daysAgo(now, 2)),
                ),
            ),
            LabItem(
                id = UUID.randomUUID().toString(),
                name = "염산 1M",
                type = LabItemType.REAGENT,
                category = "산/염기",
                spec = "1L",
                unit = "L",
                currentQuantity = 0.6,
                minimumQuantity = 0.8,
                storageLocation = "시약장 B-2",
                manager = "화학실",
                hazardLevel = HazardLevel.HIGH,
                expiryDate = "2026-07-20",
                note = "중화 반응 실험 전 재주문 필요",
                lastCheckedAt = daysAgo(now, 0),
                history = listOf(
                    seededEvent(InventoryEventType.STOCK_OUT, 0.2, "산염기 적정", daysAgo(now, 7)),
                    seededEvent(InventoryEventType.AUDIT, 0.0, "라벨 및 밀봉 상태 확인", daysAgo(now, 1)),
                ),
            ),
            LabItem(
                id = UUID.randomUUID().toString(),
                name = "수산화나트륨 펠릿",
                type = LabItemType.REAGENT,
                category = "산/염기",
                spec = "500g",
                unit = "kg",
                currentQuantity = 0.4,
                minimumQuantity = 0.3,
                storageLocation = "시약장 B-4",
                manager = "화학실",
                hazardLevel = HazardLevel.HIGH,
                expiryDate = "2027-03-01",
                note = "흡습 방지를 위해 사용 후 즉시 밀봉",
                lastCheckedAt = daysAgo(now, 3),
                history = listOf(
                    seededEvent(InventoryEventType.STOCK_IN, 0.5, "신규 구매", daysAgo(now, 35)),
                    seededEvent(InventoryEventType.STOCK_OUT, 0.1, "비누화 실험", daysAgo(now, 5)),
                ),
            ),
            LabItem(
                id = UUID.randomUUID().toString(),
                name = "베네딕트 용액",
                type = LabItemType.REAGENT,
                category = "생명과학",
                spec = "2L",
                unit = "L",
                currentQuantity = 1.2,
                minimumQuantity = 0.8,
                storageLocation = "준비실 C-1",
                manager = "생명과학실",
                hazardLevel = HazardLevel.MEDIUM,
                expiryDate = "2026-08-15",
                note = "환원당 검출용",
                lastCheckedAt = daysAgo(now, 5),
                history = listOf(
                    seededEvent(InventoryEventType.STOCK_OUT, 0.3, "효소 실험", daysAgo(now, 9)),
                ),
            ),
            LabItem(
                id = UUID.randomUUID().toString(),
                name = "pH 측정기",
                type = LabItemType.EQUIPMENT,
                category = "측정기기",
                spec = "휴대용",
                unit = "대",
                currentQuantity = 2.0,
                minimumQuantity = 1.0,
                storageLocation = "화학실 장비함",
                manager = "과학부",
                hazardLevel = HazardLevel.LOW,
                expiryDate = "",
                note = "전극 세척액 잔량 주기 확인",
                lastCheckedAt = daysAgo(now, 8),
                history = listOf(
                    seededEvent(InventoryEventType.AUDIT, 0.0, "교정 완료", daysAgo(now, 8)),
                ),
            ),
            LabItem(
                id = UUID.randomUUID().toString(),
                name = "슬라이드 글라스",
                type = LabItemType.SUPPLY,
                category = "소모품",
                spec = "72매",
                unit = "팩",
                currentQuantity = 1.0,
                minimumQuantity = 2.0,
                storageLocation = "생명실 서랍 2",
                manager = "생명과학실",
                hazardLevel = HazardLevel.LOW,
                expiryDate = "",
                note = "현미경 실습 주간 전 추가 확보 권장",
                lastCheckedAt = daysAgo(now, 2),
                history = listOf(
                    seededEvent(InventoryEventType.STOCK_OUT, 1.0, "세포 관찰 수업", daysAgo(now, 4)),
                ),
            ),
        ).sortedBy { it.name.lowercase() }
    }

    private fun JSONArray?.toEvents(): List<InventoryEvent> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val event = getJSONObject(index)
                add(
                    InventoryEvent(
                        id = event.optString("id"),
                        type = event.optEnum("type", InventoryEventType.AUDIT),
                        amount = event.optDouble("amount", 0.0),
                        memo = event.optString("memo"),
                        happenedAt = event.optLong("happenedAt", System.currentTimeMillis()),
                    ),
                )
            }
        }.sortedByDescending { it.happenedAt }
    }

    private fun seededEvent(type: InventoryEventType, amount: Double, memo: String, happenedAt: Long): InventoryEvent {
        return InventoryEvent(
            id = UUID.randomUUID().toString(),
            type = type,
            amount = amount,
            memo = memo,
            happenedAt = happenedAt,
        )
    }

    private fun daysAgo(now: Long, days: Long): Long = now - TimeUnit.DAYS.toMillis(days)

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
        return enumValues<T>().firstOrNull { it.name == optString(key, fallback.name) } ?: fallback
    }

    companion object {
        private const val PREFS_NAME = "reagent_keeper_store"
        private const val KEY_ITEMS = "inventory_items"
    }
}
