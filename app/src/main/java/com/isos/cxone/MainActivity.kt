package com.isos.cxone

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isos.cxone.ui.theme.CxoneSampleTheme
import com.nice.cxonechat.ChatInstanceProvider
import com.isos.cxone.viewmodel.ChatConversationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.exceptions.RuntimeChatException
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity(), ChatInstanceProvider.Listener {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Renamed to liveChatState to clearly indicate it is the mutable, live source of the Compose state.
    private val liveChatState = mutableStateOf<ChatState>(Initial)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CxoneSampleTheme {
                // Pass the current value of the live state holder to the Composable
                MainScreen(currentChatState = liveChatState.value)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register the listener when the activity becomes visible
        try {
            ChatInstanceProvider.get().addListener(this)
            Log.d(TAG, "ChatInstanceProvider listener registered.")
        } catch (e: IllegalStateException) {
            // This happens if the ChatInstanceProvider.create() call failed in the Application class
            Log.e(TAG, "SDK not initialized. Check application class.", e)
            Toast.makeText(this, "SDK not initialized.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // Unregister the listener when the activity is no longer visible to prevent leaks
        try {
            ChatInstanceProvider.get().removeListener(this)
            Log.d(TAG, "ChatInstanceProvider listener unregistered.")
        } catch (e: IllegalStateException) {
            // Ignore if the instance was never created
        }
    }

    // --- ChatInstanceProvider.Listener Implementation (The Automatic State Machine) ---

    /**
     * The primary callback for all chat state changes.
     */
    override fun onChatStateChanged(chatState: ChatState) {
        Log.i(TAG, "Chat State Changed: $chatState")

        // 2. Update the Live State: Updating this MutableState triggers recomposition in MainScreen.
        liveChatState.value = chatState

        // Ensure all UI operations run on the Main Thread
        runOnUiThread {
            try {
                val provider = ChatInstanceProvider.get()
                // This is where we handle the state machine logic
                when (chatState) {
                    Initial -> {
                        // ChatInstanceProvider wasn't initialized yet (or was explicitly reset).
                        Toast.makeText(
                            this,
                            "SDK Initial. Calling prepare()...",
                            Toast.LENGTH_SHORT
                        ).show()
                        provider.prepare(this@MainActivity)
                    }

                    Preparing -> {
                        // ChatInstanceProvider is being configured.
                        Toast.makeText(this, "Preparing SDK configuration...", Toast.LENGTH_SHORT)
                            .show()
                    }

                    Prepared -> {
                        // Key step: Once prepared, immediately initiate the socket connection automatically
                        Toast.makeText(
                            this,
                            "SDK Prepared. Calling connect()... (Automatic)",
                            Toast.LENGTH_SHORT
                        ).show()
                        try {
                            provider.connect()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calling connect() from PREPARED state", e)
                        }
                    }

                    Connecting -> {
                        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
                    }

                    Connected -> {
                        Toast.makeText(
                            this,
                            "Connected! Establishing session...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    Ready -> {
                        Toast.makeText(
                            this,
                            "Chat READY! You can now start the chat session.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    ConnectionLost -> {
                        Toast.makeText(
                            this,
                            "Connection Lost (attempting to reconnect)",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    Offline -> {
                        Toast.makeText(this, "Chat Channel is Offline.", Toast.LENGTH_LONG).show()
                    }

                }
            } catch (e: IllegalStateException) {
                // If the provider fails to initialize in the Application class, this will catch the error.
                Log.e(TAG, "ChatInstanceProvider not available in onChatStateChanged", e)
            }
        }
    }

    /**
     * Invoked when the chat object changes. Not typically used for connection flow.
     */
    override fun onChatChanged(chat: Chat?) {
        Log.d(TAG, "Chat object changed: ${chat?.javaClass?.simpleName}")
    }

    /**
     * Invoked when chat reports runtime exception.
     */
    override fun onChatRuntimeException(exception: RuntimeChatException) {
        // Since this might also be called from a background thread, ensure UI work runs on main thread
        runOnUiThread {
            Log.e(TAG, "SDK Runtime Exception: ${exception.message}", exception)
            Toast.makeText(this, "Chat Error: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Main Composable Screen: Now receives the current state as a parameter.
 */
@Composable
fun MainScreen(currentChatState: ChatState) {
    val context = LocalContext.current
    var isChatActive by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NICE CXone Chat SDK Sample",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isChatActive) {
                // --- Conditional ViewModel Initialization ---
                val chatViewModel: ChatConversationViewModel = viewModel()
                ChatSessionScreen(chatViewModel)
                // ------------------------------------------
            } else {
                // Pass the live state to the status screen
                ConnectionStatusScreen(currentChatState) {
                    // Manual connection/prepare check logic
                    try {
                        val provider = ChatInstanceProvider.get()
                        val currentState = provider.chatState

                        val message = when (currentState) {
                            Initial -> {
                                // Manual start: Kick off the automatic preparation flow
                                provider.prepare(context)
                                "Initial. Attempting Prepare()..."
                            }
                            Prepared -> {
                                // State is Prepared, but we rely on the automatic flow in the listener to call connect()
                                "Prepared. Waiting for automatic Connect()..."
                            }
                            Ready -> {
                                // If already ready, start the chat session immediately
                                isChatActive = true
                                "Chat is READY! Starting session..."
                            }
                            Connecting, Connected, Preparing -> "Connection/Preparation in progress ($currentState). Button action skipped."
                            else -> "Current State: $currentState. Waiting for state change..."
                        }
                        Log.i("MainScreen", message)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                    } catch (e: IllegalStateException) {
                        val message = "Error: SDK not initialized. Check logs."
                        Log.e("MainScreen", message, e)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        val message = "Connect error: ${e.message}"
                        Log.e("MainScreen", message, e)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                // New button for starting the chat session, only visible when Ready
                if (currentChatState == Ready) { // Use the live state here
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        // This sets the state to trigger the ViewModel instantiation
                        isChatActive = true
                        Toast.makeText(context, "Starting Chat Session...", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Start Chat Session")
                    }
                }
            }
        }
    }
}

/**
 * Displays the current connection status and the main connection button.
 */
@Composable
fun ConnectionStatusScreen(chatState: ChatState, onActionClick: () -> Unit) {
    val statusText = "Current State: $chatState"

    val statusColor = when (chatState) {
        Ready, Connected -> MaterialTheme.colorScheme.primary
        Connecting, Preparing -> Color.Blue
        ConnectionLost, Offline -> MaterialTheme.colorScheme.error
        else -> Color.Gray
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.titleLarge,
        color = statusColor,
        modifier = Modifier.padding(bottom = 32.dp)
    )

    // Button to manually check state and try to connect/prepare
    Button(onClick = onActionClick) {
        Text("Check & Manage Connection")
    }
}

/**
 * Placeholder for the actual Chat UI when the session is active.
 * This is where the ViewModel is used.
 */
@Composable
fun ChatSessionScreen(viewModel: ChatConversationViewModel) {
    // Collect the thread StateFlow to automatically update the UI when the thread changes
    val chatThread by viewModel.thread.collectAsState()

    // Determine the status text based on the presence of the thread
    val threadStatus = if (chatThread != null) {
        "Thread ID: ${chatThread?.id}"
    } else {
        "Waiting for Chat Thread..."
    }

    // Send an example message when the thread is available
    if (chatThread != null) {
        viewModel.sendExampleMessage()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Chat Session Active!",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = threadStatus,
            style = MaterialTheme.typography.bodyLarge,
            color = if (chatThread != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CxoneSampleTheme {
        // Must provide a default state for the preview
        MainScreen(currentChatState = Initial)
    }
}
