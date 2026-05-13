package com.example.auctornotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auctornotes.data.model.Project
import com.example.auctornotes.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: NoteViewModel,
    syncRepository: com.example.auctornotes.sync.SyncRepository,
    onProjectClick: (Project) -> Unit
) {
    val projects by viewModel.allProjects.collectAsState(initial = emptyList())
    val isConnected by syncRepository.isConnected.collectAsState()
    val isSearching by syncRepository.isSearching.collectAsState()
    val activeProjectName by syncRepository.activeProjectName.collectAsState()
    val connectionError by syncRepository.connectionError.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var showManualConnectDialog by remember { mutableStateOf(false) }
    var manualAddress by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("AuctorNotes", style = MaterialTheme.typography.headlineMedium)
                        when {
                            isConnected -> {
                                Text(
                                    "Connected" + (activeProjectName?.let { " - $it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            isSearching -> {
                                Text(
                                    "Searching for PC... (IP: ${syncRepository.getLocalIpAddress()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            else -> {
                                if (connectionError != null) {
                                    Text(
                                        "Failed: $connectionError",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        "Not Connected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (isConnected || isSearching) {
                        TextButton(onClick = { syncRepository.stop() }) {
                            Text("Stop Pairing")
                        }
                    } else {
                        TextButton(
                            onClick = { showManualConnectDialog = true }
                        ) {
                            Text("Manual")
                        }
                        Button(
                            onClick = { syncRepository.start() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Pair to PC")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Project", modifier = Modifier.size(36.dp))
            }
        }
    ) { innerPadding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No projects yet. Add one to get started!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "My Projects",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(projects) { project ->
                    ProjectItem(
                        project = project,
                        onClick = { onProjectClick(project) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Project") },
            text = {
                TextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.addProject(newProjectName)
                            newProjectName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManualConnectDialog) {
        AlertDialog(
            onDismissRequest = { showManualConnectDialog = false },
            title = { Text("Connect Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the IP address and port shown in the Auctor desktop app (e.g. 192.168.1.5:48123)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextField(
                        value = manualAddress,
                        onValueChange = { manualAddress = it },
                        label = { Text("IP:Port") },
                        placeholder = { Text("192.168.1.5:48123") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parts = manualAddress.trim().split(":")
                        if (parts.size == 2) {
                            val host = parts[0].trim()
                            val port = parts[1].trim().toIntOrNull()
                            if (host.isNotEmpty() && port != null) {
                                syncRepository.connectManually(host, port)
                                showManualConnectDialog = false
                            }
                        }
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProjectItem(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Project",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
