package com.isos.cxone

import android.app.Application
import android.util.Log
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.enums.CXoneEnvironment
import com.nice.cxonechat.state.Environment

class CxoneSampleApplication : Application() {
    private val TAG = "CxoneApplication"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate - Initializing NICE CXone Chat SDK")

        // 1. Create the correct Configuration object using your parameters
        // Region: EU1, BrandId: 1086L, ChannelId: chat_15bf234b-d6a8-4ce0-8b90-e8cf3c6f3748
        val config = SocketFactoryConfiguration(
            CXoneEnvironment.EU1.value,
            1086L, // Brand ID must be a Long
            "chat_15bf234b-d6a8-4ce0-8b90-e8cf3c6f3748"
        )

        // 2. Create the singleton ChatInstanceProvider
        try {
            // Note: This method automatically creates the singleton instance and logs the state.
            ChatInstanceProvider.create(config)
            Log.i(TAG, "ChatInstanceProvider created successfully.")

            // 3. Immediately prepare the connection
            // Calling prepare starts the asynchronous process of building the Chat instance.
            ChatInstanceProvider.get().prepare(this)
            Log.i(TAG, "ChatInstanceProvider prepare() initiated.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NICE CXone Chat SDK: ${e.message}", e)
        }
    }
}