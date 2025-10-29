package com.isos.cxone.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.message.OutboundMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatConversationViewModel : ViewModel() {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val chat = ChatInstanceProvider.get().chat.also {
        Log.d(TAG, "Chat instance obtained: $it")
    }
    private val chatThreadsHandler: ChatThreadsHandler? = chat?.threads()?.also {
        Log.d(TAG, "ChatThreadsHandler initialized.")
    }
    private var chatThreadHandler: ChatThreadHandler? = null

    private var cancellableThreadsCallback: Cancellable? = null
    private var cancellableThreadCallback: Cancellable? = null

    private var isCreatingThread = false
    private var hasThreadAttached = false

    private val _thread = MutableStateFlow<ChatThread?>(null)
    val thread = _thread.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized → setting up thread listener.")
        observeThreads()
    }

    fun sendExampleMessage() {
        val messageHandler = chatThreadHandler?.messages() ?: run {
            Log.e(TAG, "Cannot send message → ChatThreadHandler or MessagesHandler is null.")
            return
        }
        messageHandler.send(OutboundMessage("Creating new thread!"))
    }

    private fun observeThreads() {
        val handler = chatThreadsHandler ?: run {
            Log.e(TAG, "ChatThreadsHandler is null → Chat not ready.")
            return
        }

        Log.v(TAG, "Attaching persistent threads() listener...")

        // PROBLEM: This threads callback is not fired again? Even though a new thread is created.
        cancellableThreadsCallback = handler.threads { threadsList ->
            Log.d(TAG, "→ threads() callback fired with count=${threadsList.size}")

            if (threadsList.isNotEmpty()) {
                // ✅ Thread exists → safe to clear creation flag
                isCreatingThread = false
                return@threads
            }

            // No threads yet — try creating only once
            if (isCreatingThread) {
                Log.v(TAG, "Thread creation already in progress → skipping create().")
                return@threads
            }

            isCreatingThread = true
            Log.i(TAG, "No existing threads → creating one via handler.create()")

                try {
                    chatThreadHandler = handler.create()
                    Log.v(TAG, "create() returned handler=${chatThreadHandler.hashCode()}")

                    // QUESTION: Why is this the only way to get a thread?
//                    val thread = chatThreadHandler!!.get()
//                    Log.i(TAG, "New thread created with id=${thread.id}.")
//                    _thread.value = thread

                    // QUESTION: Why doesn't this work here?
                    attachThreadFlow(chatThreadHandler!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during create()", e)
                    isCreatingThread = false // Reset only on error
                }
            }
    }


    private fun attachThreadFlow(handler: ChatThreadHandler) {
        if (hasThreadAttached) {
            Log.v(TAG, "Thread already attached → skipping duplicate subscription.")
            return
        }

        hasThreadAttached = true
        Log.d(TAG, "Subscribing to thread handler (hash=${handler.hashCode()}) via get()")

        cancellableThreadCallback = handler.get { thread ->
            Log.d(TAG, "→ threadFlow() callback → threadId=${thread.id}")
            _thread.value = thread
        }
    }

    override fun onCleared() {
        Log.w(TAG, "ViewModel cleared → cancelling listeners.")
        try {
            cancellableThreadsCallback?.cancel()
            cancellableThreadCallback?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling listeners", e)
        }
        super.onCleared()
    }
}
