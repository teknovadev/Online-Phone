package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChatInputField
import com.example.ui.components.ChatTopBar
import com.example.ui.components.EmptyChatView
import com.example.ui.components.HistorySheet
import com.example.ui.components.MessageBubble
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TypingIndicator
import com.example.ui.theme.BackgroundDark
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val isApiKeyConfigured by viewModel.apiKeyConfigured.collectAsStateWithLifecycle()
    val snackbarEvent by viewModel.snackbarEvent.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Handle snackbar events
    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Auto-scroll to the bottom when new message arrives or loading begins
    LaunchedEffect(messages.size, isLoading) {
        val totalItems = messages.size + if (isLoading) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                historyCount = conversations.size,
                isApiKeySet = isApiKeyConfigured,
                onOpenHistory = { viewModel.setShowHistory(true) },
                onNewChat = { viewModel.startNewChat() },
                onOpenSettings = { viewModel.setShowSettings(true) }
            )
        },
        bottomBar = {
            ChatInputField(
                text = inputText,
                isLoading = isLoading,
                onTextChanged = { viewModel.updateInputText(it) },
                onSendMessage = { viewModel.sendMessage() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundDark)
        ) {
            if (messages.isEmpty() && !isLoading) {
                EmptyChatView(
                    onSelectPrompt = { prompt ->
                        viewModel.sendMessage(prompt)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    itemsIndexed(messages, key = { _, item -> item.id }) { index, msg ->
                        val isLastAssistant = msg.role == "model" &&
                            index == messages.indexOfLast { it.role == "model" }

                        MessageBubble(
                            message = msg,
                            isLastAssistantMessage = isLastAssistant,
                            onCopy = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                viewModel.triggerSnackbar("Cevap panoya kopyalandı")
                            },
                            onRegenerate = {
                                viewModel.regenerateLastResponse()
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Chat History Modal Bottom Sheet
    if (showHistory) {
        HistorySheet(
            conversations = conversations,
            activeConversationId = activeConversationId,
            sheetState = historySheetState,
            onDismiss = {
                coroutineScope.launch {
                    historySheetState.hide()
                    viewModel.setShowHistory(false)
                }
            },
            onSelectConversation = { convId ->
                viewModel.selectConversation(convId)
            },
            onDeleteConversation = { convId ->
                viewModel.deleteConversation(convId)
            },
            onClearAllHistory = {
                viewModel.clearAllHistory()
            },
            onNewChat = {
                viewModel.startNewChat()
            }
        )
    }

    // Settings & API Key Dialog
    if (showSettings) {
        SettingsDialog(
            isApiKeyConfigured = isApiKeyConfigured,
            currentCustomKey = viewModel.getStoredCustomApiKey(),
            onSaveKey = { key ->
                viewModel.saveCustomApiKey(key)
            },
            onDismiss = {
                viewModel.setShowSettings(false)
            }
        )
    }
}
