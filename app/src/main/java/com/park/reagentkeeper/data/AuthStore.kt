package com.park.reagentkeeper.data

import android.content.Context
import android.util.Base64
import com.park.reagentkeeper.model.AppUser
import com.park.reagentkeeper.model.LoginResult
import com.park.reagentkeeper.model.RegistrationResult
import com.park.reagentkeeper.model.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun loadUsers(): List<AppUser> {
        val raw = preferences.getString(KEY_USERS, null) ?: return emptyList()

        return runCatching { parseUsers(JSONArray(raw)) }
            .getOrElse { emptyList() }
    }

    fun isSetupRequired(): Boolean {
        val credentials = loadCredentials()
        return loadUsers().none { user ->
            user.active && user.role == UserRole.ADMIN && credentials.containsKey(user.id)
        }
    }

    fun currentUser(): AppUser? {
        if (isSetupRequired()) {
            logout()
            return null
        }

        val currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null) ?: return null
        return loadUsers().firstOrNull { it.id == currentUserId && it.active }
    }

    fun createInitialAdmin(
        name: String,
        email: String,
        password: String,
        labName: String,
    ): LoginResult {
        if (!isSetupRequired()) {
            return LoginResult(null, "이미 관리자 계정이 설정되어 있습니다.")
        }

        val normalizedEmail = email.normalizedEmail()
        validateUserInput(name, normalizedEmail, password, labName)?.let {
            return LoginResult(null, it)
        }

        val admin = AppUser(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = normalizedEmail,
            role = UserRole.ADMIN,
            labName = labName.trim(),
            active = true,
            lastLoginAt = System.currentTimeMillis(),
        )

        saveUsers(listOf(admin))
        saveCredentials(mapOf(admin.id to createCredential(admin.id, password)))
        preferences.edit().putString(KEY_CURRENT_USER_ID, admin.id).apply()
        return LoginResult(admin, null)
    }

    fun createUser(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        labName: String,
    ): RegistrationResult {
        val normalizedEmail = email.normalizedEmail()
        val existingUsers = loadUsers()

        validateUserInput(name, normalizedEmail, password, labName)?.let {
            return RegistrationResult(null, it)
        }
        if (existingUsers.any { it.email.equals(normalizedEmail, ignoreCase = true) }) {
            return RegistrationResult(null, "이미 등록된 이메일입니다.")
        }

        val user = AppUser(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = normalizedEmail,
            role = role,
            labName = labName.trim(),
            active = true,
            lastLoginAt = 0L,
        )
        saveUsers(existingUsers + user)
        saveCredentials(loadCredentials() + (user.id to createCredential(user.id, password)))
        return RegistrationResult(user, null)
    }

    fun login(email: String, password: String): LoginResult {
        if (isSetupRequired()) {
            return LoginResult(null, "먼저 관리자 계정을 만들어 주세요.")
        }

        val normalizedEmail = email.normalizedEmail()
        val user = loadUsers().firstOrNull { it.email.equals(normalizedEmail, ignoreCase = true) }
        val credential = user?.let { loadCredentials()[it.id] }

        return when {
            user == null -> LoginResult(null, "등록된 계정을 찾을 수 없습니다.")
            !user.active -> LoginResult(null, "비활성화된 계정입니다. 관리자에게 문의해 주세요.")
            credential == null -> LoginResult(null, "계정 보안 정보가 없습니다. 관리자에게 새 계정을 요청해 주세요.")
            !verifyPassword(password, credential) -> LoginResult(null, "비밀번호를 다시 확인해 주세요.")
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
        users
            .sortedWith(compareByDescending<AppUser> { it.role == UserRole.ADMIN }.thenBy { it.name })
            .forEach { user ->
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

    private fun loadCredentials(): Map<String, PasswordCredential> {
        val raw = preferences.getString(KEY_CREDENTIALS, null) ?: return emptyMap()
        return runCatching {
            val payload = JSONArray(raw)
            buildMap {
                for (index in 0 until payload.length()) {
                    val credential = payload.getJSONObject(index)
                    val parsed = PasswordCredential(
                        userId = credential.optString("userId"),
                        salt = credential.optString("salt"),
                        hash = credential.optString("hash"),
                    )
                    if (parsed.userId.isNotBlank() && parsed.salt.isNotBlank() && parsed.hash.isNotBlank()) {
                        put(parsed.userId, parsed)
                    }
                }
            }
        }.getOrElse { emptyMap() }
    }

    private fun saveCredentials(credentials: Map<String, PasswordCredential>) {
        val payload = JSONArray()
        credentials.values.forEach { credential ->
            payload.put(
                JSONObject().apply {
                    put("userId", credential.userId)
                    put("salt", credential.salt)
                    put("hash", credential.hash)
                },
            )
        }
        preferences.edit().putString(KEY_CREDENTIALS, payload.toString()).apply()
    }

    private fun createCredential(userId: String, password: String): PasswordCredential {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return PasswordCredential(
            userId = userId,
            salt = salt.toBase64(),
            hash = hashPassword(password, salt),
        )
    }

    private fun verifyPassword(password: String, credential: PasswordCredential): Boolean {
        return runCatching {
            val salt = credential.salt.fromBase64()
            val expected = credential.hash.fromBase64()
            val actual = hashPassword(password, salt).fromBase64()
            MessageDigest.isEqual(expected, actual)
        }.getOrDefault(false)
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH)
        return try {
            SecretKeyFactory
                .getInstance(PASSWORD_ALGORITHM)
                .generateSecret(spec)
                .encoded
                .toBase64()
        } finally {
            spec.clearPassword()
        }
    }

    private fun validateUserInput(
        name: String,
        normalizedEmail: String,
        password: String,
        labName: String,
    ): String? {
        return when {
            name.isBlank() -> "이름을 입력해 주세요."
            !EMAIL_REGEX.matches(normalizedEmail) -> "사용할 수 있는 이메일 형식으로 입력해 주세요."
            password.length < MIN_PASSWORD_LENGTH -> "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상으로 입력해 주세요."
            labName.isBlank() -> "학교 또는 실험실 이름을 입력해 주세요."
            else -> null
        }
    }

    private fun String.normalizedEmail(): String = trim().lowercase()

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
        return enumValues<T>().firstOrNull { it.name == optString(key, fallback.name) } ?: fallback
    }

    private data class PasswordCredential(
        val userId: String,
        val salt: String,
        val hash: String,
    )

    companion object {
        private const val PREFS_NAME = "ontect_auth_store"
        private const val KEY_USERS = "users"
        private const val KEY_CREDENTIALS = "credentials"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val PASSWORD_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PASSWORD_ITERATIONS = 120_000
        private const val PASSWORD_KEY_LENGTH = 256
        private const val SALT_BYTES = 16
        private const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}
