package com.duriseo.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.data.local.cleanup.LocalDataCleaner
import com.duriseo.android.data.local.db.DuriseoDatabase
import com.duriseo.android.data.local.me.MeLocalDataSource
import com.duriseo.android.data.remote.auth.AuthApi
import com.duriseo.android.data.remote.auth.AuthRemoteDataSource
import com.duriseo.android.data.remote.auth.AuthSessionApi
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.me.MeApi
import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.auth.AuthRepositoryImpl
import com.duriseo.android.data.repository.auth.SessionManager
import com.duriseo.android.data.repository.me.MeRepositoryImpl
import com.duriseo.android.data.repository.me.toEntity
import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.SocialSessionCleaner
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

/** Room(Robolectric 메모리 DB)과 실제 Retrofit(MockWebServer)을 쓰는 테스트는 실제 스레드를 써서 runBlocking으로 돈다 */
fun runTestWithTimeout(block: suspend CoroutineScope.() -> Unit) {
    runBlocking { withTimeout(20_000) { block() } }
}

class FakeNetworkMonitor(var online: Boolean = true) : NetworkMonitor {
    override val isOnline: Flow<Boolean> get() = flowOf(online)
    override fun isOnlineNow(): Boolean = online
}

class FakeTokenStorage(initial: AuthTokens? = null) : TokenStorage {
    private val mutableState = MutableStateFlow<TokenState>(
        if (initial != null) TokenState.Present(initial) else TokenState.Loading,
    )
    override val state: StateFlow<TokenState> = mutableState

    override suspend fun restore() {
        if (mutableState.value == TokenState.Loading) mutableState.value = TokenState.Empty
    }

    override suspend fun save(tokens: AuthTokens) {
        mutableState.value = TokenState.Present(tokens)
    }

    override suspend fun clear() {
        mutableState.value = TokenState.Empty
    }
}

class FakeSocialCleaner : SocialSessionCleaner {
    val clearCount = AtomicInteger()
    override suspend fun clear() {
        clearCount.incrementAndGet()
    }
}

fun testTokens(access: String = "guest-access", refresh: String = "guest-refresh") = AuthTokens(
    accessToken = access,
    accessTokenExpiresAt = Instant.parse("2026-10-30T01:00:00Z"),
    refreshToken = refresh,
    refreshTokenExpiresAt = Instant.parse("2026-11-29T00:00:00Z"),
)

fun authTokensJson(access: String, refresh: String, isNewUser: Boolean = false): String =
    """{"accessToken":"$access","accessTokenExpiresAt":"2026-10-30T02:00:00Z",
        "refreshToken":"$refresh","refreshTokenExpiresAt":"2026-11-29T00:00:00Z","isNewUser":$isNewUser}"""

fun errorJson(code: String): String =
    """{"code":"$code","message":"개발자용 설명","details":null,"traceId":"trace-1"}"""

fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
    .setResponseCode(code)
    .setHeader("Content-Type", "application/json")
    .setBody(body)

fun meJson(
    id: String = "11111111-1111-1111-1111-111111111111",
    accountType: String = "GUEST",
    timeZone: String = "Asia/Seoul",
): String =
    """{"id":"$id","accountType":"$accountType","nickname":"게스트",
        "timeZone":"$timeZone","providers":[],"hasProfileImage":false,"createdAt":"2026-10-12T03:00:00Z",
        "space":{"id":"22222222-2222-2222-2222-222222222222","type":"PERSONAL","myColor":"pick-orange",
        "partner":null,"togetherColor":null,"startDate":null,"importPending":false}}"""

fun fixtureMe(
    id: String = "11111111-1111-1111-1111-111111111111",
    accountType: AccountType = AccountType.GUEST,
    timeZone: String = "Asia/Seoul",
) = Me(
    id = id,
    accountType = accountType,
    nickname = "게스트",
    timeZone = timeZone,
    providers = emptyList(),
    hasProfileImage = false,
    createdAt = Instant.parse("2026-10-12T03:00:00Z"),
    space = MeSpace(
        id = "22222222-2222-2222-2222-222222222222",
        type = SpaceType.PERSONAL,
        myColor = "pick-orange",
        partner = null,
        togetherColor = null,
        startDate = null,
        importPending = false,
    ),
)

/**
 * 리포지토리 테스트 한 벌: 메모리 Room + 임시 설정 DataStore + MockWebServer를 가리키는 실제 Retrofit.
 * 인증 헤더·Authenticator는 data:remote 테스트가 확인하므로 여기서는 붙이지 않는다.
 */
class RepoHarness(
    val server: MockWebServer,
    tempDir: File,
    online: Boolean = true,
    initialTokens: AuthTokens? = null,
    zoneId: String = "Asia/Seoul",
) {
    val monitor = FakeNetworkMonitor(online)
    val tokens = FakeTokenStorage(initialTokens)
    val social = FakeSocialCleaner()
    val settingsKey = stringPreferencesKey("sample_setting")

    val database: DuriseoDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        DuriseoDatabase::class.java,
    ).allowMainThreadQueries().build() // Robolectric 테스트 스레드는 메인 스레드라서

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = ioScope,
        produceFile = { File(tempDir, "settings.preferences_pb") },
    )

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(server.url("/api/v1/"))
        .addConverterFactory(createApiJson().asConverterFactory("application/json".toMediaType()))
        .client(OkHttpClient.Builder().retryOnConnectionFailure(false).build())
        .build()
    private val apiCaller = ApiCaller(createApiJson())

    val meLocal = MeLocalDataSource(database.meDao())
    val meRemote = MeRemoteDataSource(retrofit.create(MeApi::class.java), apiCaller)
    val authRemote = AuthRemoteDataSource(
        retrofit.create(AuthApi::class.java),
        retrofit.create(AuthSessionApi::class.java),
        apiCaller,
    )
    val session = SessionManager(
        tokenStorage = tokens,
        localData = LocalDataCleaner(database, settings, Dispatchers.IO),
        social = social,
        scope = ioScope,
    )
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneId.of(zoneId))
    val meRepository = MeRepositoryImpl(meRemote, meLocal, monitor)
    val authRepository = AuthRepositoryImpl(authRemote, meRemote, tokens, session, meRepository, monitor, clock)

    /** Room에 내 정보를, 설정 DataStore에 값을 미리 넣어 둔다 (지워지는지 보려고) */
    suspend fun preload(me: Me = fixtureMe()) {
        meLocal.replace(me.toEntity())
        settings.edit { it[settingsKey] = "저장된 설정" }
    }

    suspend fun roomMe(): Me? = meRepository.observeMe().first()

    suspend fun settingValue(): String? = settings.data.first()[settingsKey]

    fun close() {
        database.close()
        ioScope.cancel()
    }
}
