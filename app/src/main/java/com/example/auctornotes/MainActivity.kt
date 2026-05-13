package com.example.auctornotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auctornotes.sync.SyncRepository
import com.example.auctornotes.ui.screens.NoteDetailScreen
import com.example.auctornotes.ui.screens.NoteListScreen
import com.example.auctornotes.ui.screens.ProjectListScreen
import com.example.auctornotes.ui.theme.AuctorNotesTheme
import com.example.auctornotes.viewmodel.NoteViewModel
import com.example.auctornotes.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as AuctorNotesApplication
            val viewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(app.repository)
            )

            AuctorNotesTheme {
                AuctorNotesApp(viewModel, app.syncRepository)
            }
        }
    }
}

@Composable
fun AuctorNotesApp(viewModel: NoteViewModel, syncRepository: SyncRepository) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "projects",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("projects") {
            ProjectListScreen(
                viewModel = viewModel,
                syncRepository = syncRepository,
                onProjectClick = { project ->
                    navController.navigate("notes/${project.id}")
                }
            )
        }
        composable("notes/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            NoteListScreen(
                viewModel = viewModel,
                projectId = projectId,
                onNoteClick = { noteId ->
                    navController.navigate("noteDetail/$noteId/$projectId")
                },
                onAddNoteClick = {
                    navController.navigate("noteDetail/0/$projectId")
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("noteDetail/{noteId}/{projectId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            NoteDetailScreen(
                viewModel = viewModel,
                noteId = noteId,
                projectId = projectId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
