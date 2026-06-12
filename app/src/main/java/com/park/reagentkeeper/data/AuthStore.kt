package com.park.reagentkeeper.data

import android.content.Context
import com.park.reagentkeeper.model.AppUser
import com.park.reagentkeeper.model.LoginResult
import com.park.reagentkeeper.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

class AuthStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadUsers(): List<AppUser> {
        val raw = preferences.getString(KEY_USERS, null)
        if (raw.isNullOrBlank()) {
            val seeded = seedUsers()
            saveUsers(seeded)
            return seeded
        }

        return runCatching { parseUsers(JSONArray(raw)) }
            .getOrElse {
                val seeded = seedUsers()
                saveUsers(seeded)
                seeded
            }
    }

    fun currentUser(): AppUser? {
        val currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null) ?: return null
        return loadUsers().firstOrNull { it.id == currentUserId && it.active }
    }

    fun login(email: String, password: String): LoginResult {
        val normalizedEmail = email.trim().lowercase()
        val user = loadUsers().firstOrNull { it.email.lowercase() == normalizedEmail }

        return when {
            user == null -> LoginResult(null, "등록된 계정을 찾을 수 없습니다.")
            !user.active -> LoginResult(null, "비활성화된 계정입니다. 관리자에게 문의해 주세요.")
            password != demoPasswords[normalizedEmail] -> LoginResult(null, "비밀번호를 다시 확인해 주세요.")
            else -> {
                val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
                val updatedUsers = loadUsers().map { if (it.id == updatedUser.id) updatedUser else it }
                saveUsers(updatedUsers)
                preferences.edit().putString(KEY_CURRENT_USER_ID, updatedUser.id).apply()
                LoginResult(updatedUser, null)
            }
        }
    }

    fun logout() {
        preferences.edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    private fun saveUsers(users: List<AppUser>) {
        val payload = JSONArray()
        users.forEach { user ->
            payload.put(
                JSONObject().apply {
                    put("id", user.id)
                    put("name", user.name)
                    put("email", user.email)
                    put("role", user.role.name)
                    put("labName", user.labName)
                    put("active", user.active)
                    put("lastLoginAt", user.lastLoginAt)
                },
            )
        }
        preferences.edit().putString(KEY_USERS, payload.toString()).apply()
    }

    private fun parseUsers(payload: JSONArray): List<AppUser> {
        return buildList {
            for (index in 0 until payload.length()) {
                val user = payload.getJSONObject(index)
                add(
                    AppUser(
                        id = user.optString("id"),
                        name = user.optString("name"),
                        email = user.optString("email"),
                        role = user.optEnum("role", UserRole.TEACHER),
                        labName = user.optString("labName"),
                        active = user.optBoolean("active", true),
                        lastLoginAt = user.optLong("lastLoginAt", 0L),
                    ),
                )
            }
        }.sortedWith(compareByDescending<AppUser> { it.role == UserRole.ADMIN }.thenBy { it.name })
    }

    private fun seedUsers(): List<AppUser> {
        return listOf(
            AppUser(
                id = "user-admin",
                name = "온택트 관리자",
                email = "admin@ontect.school",
                role = UserRole.ADMIN,
                labName = "과학부 전체",
                active = true,
                lastLoginAt = 0L,
            ),
            AppUser(
                id = "user-chem",
                name = "화학실 담당",
                email = "chem@ontect.school",
                role = UserRole.TEACHER,
                labName = "화학실",
                active = true,
                lastLoginAt = 0L,
            ),
            AppUser(
                id = "user-bio",
                name = "생명과학실 담당",
                email = "bio@ontect.school",
                role = UserRole.TEACHER,
                labName = "생명과학실",
                active = true,
                lastLoginAt = 0L,
            ),
            AppUser(
                id = "user-assistant",
                name = "실험 보조",
                email = "assistant@ontect.school",
                role = UserRole.ASSISTANT,
                labName = "준비실",
                active = true,
                lastLoginAt = 0L,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
        return enumValues<T>().firstOrNull { it.name == optString(key, fallback.name) } ?: fallback
    }

    companion object {
        private const val PREFS_NAME = "ontect_auth_store"
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_USER_ID = "current_user_id"

        val demoPasswords = mapOf(
            "admin@ontect.school" to "admin1234",
            "chem@ontect.school" to "chem1234",
            "bio@ontect.school" to "bio1234",
            "assistant@ontect.school" to "lab1234",
        )
    }
}
