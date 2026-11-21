package com.example.myapplication.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class FakeAuthRepositoryTest {
    private var now = 1_000_000L
    private val config = AuthConfig(rememberMeSessionDays = 30, standardSessionHours = 12, maxFailedAttempts = 3, lockoutMinutes = 5)
    private val repo = FakeAuthRepository(config, latencyMillis = 0, clock = { now })

    @Test
    fun `valid password succeeds with session length based on remember me`() = runTest {
        val long = repo.signInWithPassword("a@b.co", FakeAuthRepository.VALID_PASSWORD, rememberMe = true)
        val short = repo.signInWithPassword("a@b.co", FakeAuthRepository.VALID_PASSWORD, rememberMe = false)
        assertEquals(now + TimeUnit.DAYS.toMillis(30), (long as AuthResult.Success).session.expiresAtEpochMillis)
        assertEquals(now + TimeUnit.HOURS.toMillis(12), (short as AuthResult.Success).session.expiresAtEpochMillis)
    }

    @Test
    fun `special passwords simulate network and server errors`() = runTest {
        assertEquals(AuthResult.NetworkError, repo.signInWithPassword("a@b.co", "network", false))
        assertEquals(AuthResult.ServerError, repo.signInWithPassword("a@b.co", "server", false))
    }

    @Test
    fun `locks out after max failed attempts and unlocks after lockout period`() = runTest {
        assertEquals(AuthResult.InvalidCredentials, repo.signInWithPassword("a@b.co", "x", false))
        assertEquals(AuthResult.InvalidCredentials, repo.signInWithPassword("a@b.co", "x", false))
        assertEquals(AuthResult.LockedOut(5), repo.signInWithPassword("a@b.co", "x", false))
        // Even the right password is refused while locked.
        assertTrue(repo.signInWithPassword("a@b.co", FakeAuthRepository.VALID_PASSWORD, false) is AuthResult.LockedOut)

        now += TimeUnit.MINUTES.toMillis(5)
        assertTrue(repo.signInWithPassword("a@b.co", FakeAuthRepository.VALID_PASSWORD, false) is AuthResult.Success)
    }

    @Test
    fun `passkey and social outcomes are deterministic`() = runTest {
        assertEquals(AuthResult.NoPasskey, repo.signInWithPasskey())
        assertEquals(
            AuthResult.AccountLinkRequired(FakeAuthRepository.LINK_REQUIRED_EMAIL, SocialProvider.GOOGLE),
            repo.signInWithGoogle(),
        )
        assertTrue(repo.signInWithApple() is AuthResult.Success)
        assertTrue(repo.linkSocialAccount("a@b.co", SocialProvider.GOOGLE) is AuthResult.Success)
    }

    @Test
    fun `session toString redacts the token`() = runTest {
        val session = (repo.signInWithApple() as AuthResult.Success).session
        assertTrue(!session.toString().contains(session.accessToken))
    }
}
