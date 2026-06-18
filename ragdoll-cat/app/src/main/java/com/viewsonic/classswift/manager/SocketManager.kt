package com.viewsonic.classswift.manager

import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.api.AuthorizationApiService
import com.viewsonic.classswift.api.body.GetTokenBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.socket.JoinLessonSocketMessage
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SocketManager(
    private val accountManager: AccountManager,
    private val authorizationApiService: AuthorizationApiService
) {
    private var socket: Socket? = null
    private var socketToken: SocketToken = SocketToken()
    var connectionState: ConnectionState = ConnectionState.Uninitialized
        private set

    private val _connectionStateSharedFlow = MutableSharedFlow<ConnectionState>()
    val connectionStateSharedFlow = _connectionStateSharedFlow.asSharedFlow()

    private val _receivedEventDataFlow = MutableSharedFlow<ReceivedEventData>()
    val receivedEventDataFlow = _receivedEventDataFlow.asSharedFlow()

    private val coroutineScope = CoroutineManager.getScope(this)
    private var connectionJob: Job? = null

    // joinLessonMessage is not null, mean has class is ongoing status.
    private var joinLessonMessage: JoinLessonSocketMessage? = null

    /**
     * Establishes a Socket.IO connection
     */
    fun connect() {
        if (isConnected()) {
            Timber.d("[connect] : Has connected.")
            return
        }

        if (connectionJob?.isActive == true) {
            Timber.d("[connect] : Is connecting")
            return
        }

        connectionJob = coroutineScope.launch(Dispatchers.IO) {
            emitState(ConnectionState.Connecting)
            val userId = accountManager.userInfo.userId.takeIf { it.isNotEmpty() } ?: run {
                emitState(ConnectionState.Error(error = ConnectionError.ERROR_USER_ID_NOT_FOUND))
                return@launch
            }
            Timber.d("[connect] : userId = $userId")
            val selectedOrg = accountManager.selectedOrg ?: run {
                emitState(ConnectionState.Error(error = ConnectionError.ERROR_ORGANIZATION_ID_NOT_FOUND))
                return@launch
            }
            Timber.d("[connect] : selectedOrgId = ${selectedOrg.orgId}")
            Timber.d("[connect] : selectedOrg userDisplayName = ${selectedOrg.userDisplayName}")
            //notice, remove token isValid check,  will add the check in feature when backend ready.
            when (val response = authorizationApiService.getToken(GetTokenBody(userId = userId))) {
                is ApiResponse.Success -> {
                    socketToken = SocketToken(response.data.accessToken)
                }
                else -> {
                    emitState(ConnectionState.Error(error = ConnectionError.ERROR_REFRESH_TOKEN_FAILED))
                    return@launch
                }
            }
            Timber.d("[connect] : socketToken = $socketToken")

            val queryParams = mapOf(
                "role" to "teacher",
                "org_id" to selectedOrg.orgId,
                "client_id" to userId,
                "display_name" to selectedOrg.userDisplayName,
                "region" to "TWN",
                "stickyId" to userId,
            )
            Timber.d("[connect] : queryParams = $queryParams")
            val options = IO.Options().apply {
                path = "/sockets"

                reconnection = true // 自動重連
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000

                transports = arrayOf(WebSocket.NAME, Polling.NAME) // 支持多種傳輸方式，優先使用 websocket

                query = queryParams.toEncodedQueryString()
                auth = mapOf(
                    "access_token" to socketToken.token
                )
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .build()
                    callFactory = okHttpClient
                    webSocketFactory = okHttpClient
                }
            }
            // url 的 /teacher 要加，雖然後台因為 name space的原因，logcat回傳的url 會沒/teacher
            socket = IO.socket("${BuildConfig.BASE_URL}/teacher", options)
            socket?.let {
                // 註冊 Socket 事件
                it.on(Socket.EVENT_CONNECT) {
                    Timber.d("[connect] : EVENT_CONNECT")
                    //socket connected，if joinLessonMessage is not null，means having class is ongoing，need to send JOIN_LESSON event
                    joinLessonMessage?.let { obj ->
                        emit(EmittedEvent.JOIN_LESSON, obj.toJSONObject())
                    }
                    emitState(ConnectionState.Connected)
                }

                it.on(Socket.EVENT_DISCONNECT) {
                    Timber.d("[connect] : EVENT_DISCONNECT")
                    emitState(ConnectionState.Disconnected)
                }

                it.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Timber.d("[connect] : EVENT_CONNECT_ERROR : ${args.getOrNull(0)?.toString()}")
                    emitState(ConnectionState.Error(ConnectionError.ERROR_FROM_SERVER, args.getOrNull(0)?.toString() ?: ""))
                }

                it.on(ReceivedEvent.EVENT_TEACHER_FORCE_LOGOUT.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_TEACHER_FORCE_LOGOUT -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_TEACHER_FORCE_LOGOUT, data))
                }

                it.on(ReceivedEvent.EVENT_CHOOSE_SEAT.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_CHOOSE_SEAT -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_CHOOSE_SEAT, data))
                }

                it.on(ReceivedEvent.EVENT_REJOIN_SEAT.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_REJOIN_SEAT -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_REJOIN_SEAT, data))
                }

                it.on(ReceivedEvent.EVENT_RELEASE_SEAT.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_RELEASE_SEAT -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_RELEASE_SEAT, data))
                }

                it.on(ReceivedEvent.EVENT_SET_STUDENT_NAME.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_SET_STUDENT_NAME -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_SET_STUDENT_NAME, data))
                }

                it.on(ReceivedEvent.EVENT_SUBMIT_QUIZ.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_SUBMIT_QUIZ -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_SUBMIT_QUIZ, data))
                }

                it.on(ReceivedEvent.EVENT_STUDENT_ANSWERED.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_STUDENT_ANSWERED -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_STUDENT_ANSWERED, data))
                }

                it.on(ReceivedEvent.EVENT_TASK_RESPONSE.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_TASK_RESPONSE -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_TASK_RESPONSE, data))
                }

                it.on(ReceivedEvent.EVENT_BATCH_QUIZZES_STUDENT_SUBMITTED.nameInServer) { args ->
                    val data = args.getOrNull(0) as JSONObject?
                    Timber.d("[connect] : EVENT_BATCH_QUIZZES_STUDENT_SUBMITTED -> $data")
                    emitData(ReceivedEventData(ReceivedEvent.EVENT_BATCH_QUIZZES_STUDENT_SUBMITTED, data))
                }
                it.connect()
            }
        }
    }

    private fun emitState(state: ConnectionState) {
        coroutineScope.launch {
            connectionState = state
            _connectionStateSharedFlow.emit(state)
        }
    }

    private fun emitData(data: ReceivedEventData) {
        coroutineScope.launch {
            _receivedEventDataFlow.emit(data)
        }
    }

    fun setJoinLessonMessage(message: JoinLessonSocketMessage?) {
        joinLessonMessage = message
    }

    fun emit(event: EmittedEvent, data: JSONObject): Boolean {
        socket?.takeIf { it.connected() }?.let { validSocket ->
            Timber.tag("RandomDrawEvent").d("event = $event, data = $data")
            Timber.d("[emit] : event = $event")
            Timber.d("[emit] : data = $data")
            validSocket.emit(event.nameInServer, data)
            return true
        }
        Timber.tag("RandomDrawEvent").d("socket disconnect can't send event = $event")
        return false
    }

    //when logout, clear teacher socket.
    fun clearToken() {
        socketToken = SocketToken()
    }

    fun disconnect() {
        socket?.disconnect()
        // Commented out this code for now, as the function removes all registered socket event listeners, which may lead to timing issues.
        // We need to revisit and carefully reconsider this logic when we have more time.
        // socket?.off()
        emitState(ConnectionState.Disconnected)
    }

    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    sealed class ConnectionState {
        // This state only occurs when the user has never called connect().
        data object Uninitialized : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data object Disconnected : ConnectionState()
        data class Error(val error: ConnectionError, val message: String = "") : ConnectionState()
    }

    data class ReceivedEventData(
        val event: ReceivedEvent,
        val messageJsonObject: JSONObject?
    )

    enum class ReceivedEvent(val nameInServer: String) {
        EVENT_TEACHER_FORCE_LOGOUT("teacher_force_logout"),
        EVENT_CHOOSE_SEAT("choose_seat"),
        EVENT_RELEASE_SEAT("release_seat"),
        EVENT_REJOIN_SEAT("rejoin_seat"),
        EVENT_SET_STUDENT_NAME("set_student_name"),
        EVENT_SUBMIT_QUIZ("submit_quiz"),
        EVENT_STUDENT_ANSWERED("student_answered"),
        EVENT_TASK_RESPONSE("task_response"),
        EVENT_BATCH_QUIZZES_STUDENT_SUBMITTED("batch_quizzes_student_submitted"),
    }

    enum class EmittedEvent(val nameInServer: String) {
        TEACHER_LOGOUT("teacher_logout"),
        JOIN_LESSON("join_lesson"),
        SELECT_STUDENT("select_student"),
        START_RACE("start_race"),
        END_RACE("end_race")
    }

    enum class ConnectionError {
        ERROR_USER_ID_NOT_FOUND,
        ERROR_ORGANIZATION_ID_NOT_FOUND,
        ERROR_REFRESH_TOKEN_FAILED,
        ERROR_FROM_SERVER,
    }

    data class SocketToken(
        val token: String = "",
        val createdDateInMillis: Long = System.currentTimeMillis()
    ) {
        /**
         *  The socket token is only valid for 24 hours.
         */
        fun isValid(): Boolean {
            if (token.isEmpty()) {
                return false
            }
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - createdDateInMillis
            val twentyFourHoursInMillis = TimeUnit.HOURS.toMillis(24)
            return elapsedTime < twentyFourHoursInMillis
        }
    }
}

fun Map<String, String>.toEncodedQueryString(): String {
    return this.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
    }
}
