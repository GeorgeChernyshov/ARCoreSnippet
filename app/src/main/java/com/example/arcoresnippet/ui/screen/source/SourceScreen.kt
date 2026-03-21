package com.example.arcoresnippet.ui.screen.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.arcoresnippet.R
import com.example.arcoresnippet.domain.model.Recording
import com.example.arcoresnippet.ui.theme.ARCoreSnippetTheme

@Composable
fun SourceScreen(
    onCameraClick: () -> Unit,
    onRecordingClick: (String) -> Unit
) {
    val viewModel: SourceScreenViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRecordings()
    }

    SourceScreenContent(
        uiState = uiState,
        onCameraClick = onCameraClick,
        onShowRecordingsClick = viewModel::showRecordingsList,
        onDeleteClick = viewModel::showDeleteConfirmationDialog,
        onDeleteConfirm = {
            viewModel.deleteRecordings()
            viewModel.refreshRecordings()
            viewModel.hideDeleteConfirmationDialog()
        },
        onDeleteDismiss = viewModel::hideDeleteConfirmationDialog,
        onRecordingClick = onRecordingClick
    )
}

@Composable
fun SourceScreenContent(
    uiState: SourceScreenState,
    onCameraClick: () -> Unit,
    onShowRecordingsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onRecordingClick: (String) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCameraClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.source_button_camera))
            }

            Button(
                onClick = onShowRecordingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.source_button_recording))
            }

            if (uiState.recordingListShown) {
                RecordingsList(
                    recordings = uiState.recordings,
                    onRecordingClick = {
                        onRecordingClick(it.fileName)
                    }
                )
            }

            Button(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.source_button_delete))
            }
        }

        if (uiState.confirmDeleteDialogShown) {
            AlertDialog(
                onDismissRequest = onDeleteDismiss,
                title = {
                    Text(stringResource(R.string.source_delete_dialog_title))
                },
                text = {
                    Text(stringResource(R.string.source_delete_dialog_text))
                },
                confirmButton = {
                    TextButton(onClick = onDeleteConfirm) {
                        Text(
                            text = stringResource(R.string.source_delete_dialog_confirm),
                            color = Color.Red
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDeleteDismiss) {
                        Text(stringResource(R.string.source_delete_dialog_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun RecordingsList(
    recordings: List<Recording>,
    onRecordingClick: (Recording) -> Unit
) {
    LazyColumn() {
        items(recordings) { recording ->
            Card(onClick = { onRecordingClick(recording) }) {
                Text(recording.fileName)
            }
        }
    }
}

@Composable
@Preview
fun RecordingsListPreview() {
    ARCoreSnippetTheme {
        RecordingsList(
            recordings = listOf(
                Recording("recording1"),
                Recording("recording2"),
                Recording("recording3")
            ),
            onRecordingClick = {}
        )
    }
}

@Composable
@Preview
fun SourceScreenContentPreview() {
    ARCoreSnippetTheme {
        SourceScreenContent(
            uiState = SourceScreenState(),
            onCameraClick = {},
            onShowRecordingsClick = {},
            onDeleteClick = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onRecordingClick = {}
        )
    }
}