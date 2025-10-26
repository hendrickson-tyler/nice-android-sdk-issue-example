package com.isos.cxone

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CxoneSampleTheme {
                MainScreen()
            }
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
                text = "NICE CXone Chat SDK Test",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(onClick = {
                try {
                    // Try to retrieve the singleton provider
                    val provider = ChatInstanceProvider.get()
                    val chatState = provider.chatState

                    // Report the current state. We expect it to be PREPARING, PREPARED, or READY
                    val message = "Provider Retrieved.\nChat State: $chatState"
                    Log.i("MainActivity", message)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                } catch (e: IllegalStateException) {
                    val message = "Error: ChatInstanceProvider not initialized. Check your Application class and Manifest."
                    Log.e("MainActivity", message, e)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val message = "Unexpected Error: ${e.message}"
                    Log.e("MainActivity", message, e)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }) {
                Text("Check Chat SDK State")
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