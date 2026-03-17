package com.wiom.csp.ui.taskdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.TaskRepository
import com.wiom.csp.data.repository.TeamRepository
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.model.TechnicianData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.renderer.TaskDetailRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wrapper that connects [TaskDetailRenderer] to the ViewModel layer.
 * Loads task from Room cache, provides technician list, and handles
 * phone call and camera intents.
 */
@Composable
fun TaskDetailScreen(
    taskId: String,
    schema: SchemaResolver,
    hindi: Boolean,
    technicians: List<TechnicianData>,
    onBack: () -> Unit,
    onAction: (String, String, Map<String, String>) -> Unit
) {
    val viewModel: TaskDetailViewModel = hiltViewModel()
    val task by viewModel.task.collectAsState()
    val context = LocalContext.current

    // Load task on entry
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    val currentTask = task
    if (currentTask == null) {
        // Loading or not found
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    TaskDetailRenderer(
        task = currentTask,
        schema = schema,
        hindi = hindi,
        technicians = technicians,
        onAction = { tId, actionId, params ->
            onAction(tId, actionId, params)
        },
        onBack = onBack,
        onCallCustomer = { phone ->
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            context.startActivity(intent)
        },
        onCallTech = { phone ->
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            context.startActivity(intent)
        },
        onAddNote = { noteText ->
            viewModel.addNote(taskId, noteText)
        },
        onRequestHelp = { reasonId ->
            viewModel.requestHelp(taskId, reasonId)
        }
    )
}

/**
 * ViewModel for the task detail screen.
 * Loads a single task from the repository (Room cache) and provides
 * helper actions for notes and help requests.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepo: TaskRepository
) : ViewModel() {

    private val _task = MutableStateFlow<TaskData?>(null)
    val task: StateFlow<TaskData?> = _task.asStateFlow()

    /**
     * Load a task by ID from the local cache (Room via repository).
     */
    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _task.value = taskRepo.getTask(taskId)
        }
    }

    /**
     * Add a quick note to the task.
     * Uses the task action endpoint with a "note" action.
     */
    fun addNote(taskId: String, noteText: String) {
        viewModelScope.launch {
            taskRepo.performAction(taskId, "add_note", mapOf("text" to noteText))
            // Refresh task after note
            _task.value = taskRepo.getTask(taskId)
        }
    }

    /**
     * Request help from Wiom for a specific reason.
     */
    fun requestHelp(taskId: String, reasonId: String) {
        viewModelScope.launch {
            taskRepo.performAction(taskId, "request_help", mapOf("reason" to reasonId))
            _task.value = taskRepo.getTask(taskId)
        }
    }
}
