package com.example.myapplication.data.auth

/** Stores the signed-in session. Implementations must never log the token. */
interface TokenStorage {
    fun saveSession(session: AuthSession)
    fun getSession(): AuthSession?
    fun clear()
}

/**
 * Keeps the session in memory only, so nothing is written to disk.
 *
 * TODO(SCRUM-1): Replace with an implementation backed by the Android Keystore
 *  (e.g. EncryptedSharedPreferences or a Keystore-wrapped DataStore) so remember-me
 *  sessions survive process death. Exclude that file from backups.
 */
class InMemoryTokenStorage : TokenStorage {
    @Volatile
    private var session: AuthSession? = null

    override fun saveSession(session: AuthSession) {
        this.session = session
    }

    override fun getSession(): AuthSession? = session

    override fun clear() {
        session = null
    }
}
