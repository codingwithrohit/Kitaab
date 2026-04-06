package com.kitaab.app.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kitaab.app.domain.model.Message
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val packedPhotoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.uploadPackedPhoto(it) }
        }
    val handoffSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.lastIndex)
                    }
                }

                is ChatEvent.TransactionComplete -> {
                    snackbarHostState.showSnackbar("🎉 Transaction complete! Don't forget to leave a review.")
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.packedPhotoUploadError) {
        state.packedPhotoUploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPackedPhotoError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            // Transaction banner — shown when transaction exists or can be initiated
            TransactionBanner(
                state = state,
                onMarkComplete = { viewModel.onMarkCompleteClick() },
                onDispute = { viewModel.raiseDispute() },
                onPickPackedPhoto = { packedPhotoLauncher.launch("image/*") },
            )

            when {
                state.isLoading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        CircularProgressIndicator(color = Teal500)
                    }
                }

                state.messages.isEmpty() -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Send a message to start the conversation",
                            fontSize = 14.sp,
                            color = WarmMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                    ) {
                        items(
                            items = state.messages,
                            key = { it.id },
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderId == state.currentUserId,
                            )
                        }
                    }
                }
            }

            MessageInputBar(
                text = state.inputText,
                isSending = state.isSending,
                onTextChange = { viewModel.onInputChanged(it) },
                onSend = { viewModel.sendMessage() },
            )
        }
    }

    // Handoff method bottom sheet
    if (state.showHandoffSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissHandoffSheet() },
            sheetState = handoffSheetState,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            HandoffMethodSheet(
                onMethodSelected = { method ->
                    coroutineScope.launch {
                        handoffSheetState.hide()
                        viewModel.onHandoffMethodSelected(method)
                    }
                },
            )
        }
    }
}

@Composable
private fun TransactionBanner(
    state: ChatUiState,
    onMarkComplete: () -> Unit,
    onDispute: () -> Unit,
    onPickPackedPhoto: () -> Unit,
) {
    val transaction = state.transaction

    if (transaction?.completedAt != null && !transaction.disputed) return

    if (transaction?.disputed == true) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = "⚠️ Dispute raised",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "Our team will review this transaction.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
        }
        HorizontalDivider(color = WarmBorder)
        return
    }

    if (transaction == null && state.isSeller) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Teal50)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Ready to hand off the book?",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Teal900,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onMarkComplete,
                enabled = !state.isCreatingTransaction,
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
            ) {
                if (state.isCreatingTransaction) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text("Mark as Complete", fontSize = 13.sp)
                }
            }
        }
        HorizontalDivider(color = WarmBorder)
        return
    }

    if (transaction == null && !state.isSeller) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Teal50)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Waiting for seller to initiate handoff…",
                fontSize = 13.sp,
                color = Teal900,
            )
        }
        HorizontalDivider(color = WarmBorder)
        return
    }

    if (transaction == null) return

    val currentUserConfirmed =
        if (state.isSeller) {
            transaction.confirmedBySeller
        } else {
            transaction.confirmedByBuyer
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Teal50)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val methodLabel =
                when (transaction.handoffMethod) {
                    "MEETUP" -> "📍 Meetup"
                    "PORTER" -> "🚚 Porter / Courier"
                    "POST" -> "📦 Post / Courier"
                    else -> transaction.handoffMethod ?: ""
                }
            Text(
                text = methodLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Teal900,
            )
        }

        if (!transaction.handoffCode.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Handoff code: ${transaction.handoffCode}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Teal500,
            )
        }

        // Packed photo — seller upload + buyer view
        if (state.isSeller && !transaction.confirmedBySeller) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Thumbnail if already uploaded
                if (!transaction.packedPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = transaction.packedPhotoUrl,
                        contentDescription = "Packed book photo",
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    1.dp,
                                    Teal500.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp),
                                ),
                    )
                }

                // Upload / replace button
                OutlinedButton(
                    onClick = onPickPackedPhoto,
                    enabled = !state.isUploadingPackedPhoto,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    if (state.isUploadingPackedPhoto) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                            color = Teal500,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uploading…", fontSize = 12.sp)
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Teal500,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (transaction.packedPhotoUrl.isNullOrBlank()) {
                                "Add packed photo"
                            } else {
                                "Replace photo"
                            },
                            fontSize = 12.sp,
                            color = Teal500,
                        )
                    }
                }
            }
        }

        // Buyer sees packed photo if seller uploaded it
        if (!state.isSeller && !transaction.packedPhotoUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = transaction.packedPhotoUrl,
                    contentDescription = "Packed book photo",
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                1.dp,
                                Teal500.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp),
                            ),
                )
                Text(
                    "Seller packed photo",
                    fontSize = 12.sp,
                    color = Teal900,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text =
                buildString {
                    append("Seller: ${if (transaction.confirmedBySeller) "✓ Confirmed" else "Pending"}")
                    append("   ")
                    append("Buyer: ${if (transaction.confirmedByBuyer) "✓ Confirmed" else "Pending"}")
                },
            fontSize = 12.sp,
            color = WarmMuted,
        )

        if (!currentUserConfirmed) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMarkComplete,
                    enabled = !state.isConfirming,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    if (state.isConfirming) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text("Confirm Receipt", fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDispute,
                    enabled = !state.isDisputing,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(
                        "Dispute",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = WarmBorder)
}

@Composable
private fun HandoffMethodSheet(onMethodSelected: (String) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
    ) {
        Text(
            text = "How will you hand off the book?",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose a handoff method. This will be shared with the buyer.",
            fontSize = 13.sp,
            color = WarmMuted,
            lineHeight = 18.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))

        listOf(
            Triple("MEETUP", "📍 Meet in person", "Meet at a public place"),
            Triple(
                "PORTER",
                "🚚 Porter / Courier",
                "A 6-digit code will be generated for pickup verification",
            ),
            Triple("POST", "📦 Post / Courier", "Ship the book, buyer pays shipping"),
        ).forEach { (method, title, subtitle) ->
            TextButton(
                onClick = { onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = WarmMuted,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            HorizontalDivider(color = WarmBorder.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
) {
    val bubbleColor =
        if (isFromCurrentUser) {
            Teal500
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    val textColor =
        if (isFromCurrentUser) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val shape =
        if (isFromCurrentUser) {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp,
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            )
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .background(bubbleColor, shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatMessageTime(message.sentAt),
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text("Type a message…", color = WarmMuted, fontSize = 14.sp)
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            maxLines = 4,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal500,
                    unfocusedBorderColor = WarmBorder,
                ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        if (text.isBlank()) WarmBorder else Teal500,
                        shape = RoundedCornerShape(22.dp),
                    ),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (text.isBlank()) WarmMuted else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(isoTimestamp: String): String {
    return try {
        val sdf =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                java.util.Locale.getDefault(),
            )
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date =
            sdf.parse(isoTimestamp.substringBefore(".").substringBefore("+"))
                ?: return ""
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour =
            when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
        "$displayHour:$minute $amPm"
    } catch (e: Exception) {
        ""
    }
}
