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

class MainActivity : ComponentActivity(), ChatInstanceProvider.Listener {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CxoneSampleTheme {
                MainScreen()
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

    // --- ChatInstanceProvider.Listener Implementation ---

    /**
     * The primary callback for all chat state changes.
     */
    override fun onChatStateChanged(chatState: ChatState) {
        Log.i(TAG, "Chat State Changed: $chatState")

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
                        // The Chat object is created and ready for connection.
                        Toast.makeText(
                            this,
                            "SDK Prepared. Calling connect()...",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Key step: Once prepared, immediately initiate the socket connection
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

                    else -> {
                        // Handles any other unexpected states
                        Toast.makeText(this, "State: $chatState", Toast.LENGTH_SHORT).show()
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

@Composable
fun MainScreen() {
    val context = LocalContext.current

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

            // Button to manually check state and try to connect
            Button(onClick = {
                try {
                    val provider = ChatInstanceProvider.get()
                    val chatState = provider.chatState

                    val message = when (chatState) {
                        Initial -> {
                            provider.prepare(context)
                            "Initial. Attempting Prepare()..."
                        }

                        Prepared -> {
                            provider.connect()
                            "Prepared. Attempting Connect()..."
                        }

                        Ready -> "Chat is READY!"
                        Connecting, Connected -> "Connection in progress ($chatState)."
                        else -> "Current State: $chatState. Waiting for state change..."
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
            }) {
                Text("Check & Try Connect/Prepare")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CxoneSampleTheme {
        MainScreen()
    }
}
