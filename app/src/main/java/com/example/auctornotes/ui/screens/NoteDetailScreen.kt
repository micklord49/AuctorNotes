package com.example.auctornotes.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auctornotes.data.model.Note
import com.example.auctornotes.util.SpeechToTextManager
import com.example.auctornotes.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    projectId: Long,
    noteId: Long, // 0 for new note
    viewModel: NoteViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speechToTextManager = remember { SpeechToTextManager(context) }
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isNewNote by remember { mutableStateOf(noteId == 0L) }
    
    val isListening by speechToTextManager.isListening.collectAsState()
    val partialText by speechToTextManager.partialText.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                speechToTextManager.startListening()
            }
        }
    )

    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            val note = viewModel.getNoteById(noteId)
            note?.let {
                title = it.title
                content = it.content
                isNewNote = false
            }
        }
    }

    LaunchedEffect(Unit) {
        speechToTextManager.finalResults.collect { result ->
            if (result.isNotEmpty()) {
                content += (if (content.isNotEmpty()) " " else "") + result
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechToTextManager.destroy()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(if (isNewNote) "New Note" else "Edit Note", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (isNewNote) {
                                viewModel.addNote(projectId, title, content)
                            } else {
                                scope.launch {
                                    val note = viewModel.getNoteById(noteId)
                                    note?.let {
                                        viewModel.updateNote(it.copy(title = title, content = content, timestamp = System.currentTimeMillis()))
                                    }
                                }
                            }
                            onBackClick()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    if (isListening) {
                        speechToTextManager.stopListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                contentColor = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary,
                shape = FloatingActionButtonDefaults.largeShape
            ) {
                Icon(
                    if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Start Dictation",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Note Title", style = MaterialTheme.typography.headlineSmall) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Start typing or dictating...", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 300.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )

            if (isListening && partialText.isNotEmpty()) {
                Text(
                    text = partialText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (isListening) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 80.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Listening...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
