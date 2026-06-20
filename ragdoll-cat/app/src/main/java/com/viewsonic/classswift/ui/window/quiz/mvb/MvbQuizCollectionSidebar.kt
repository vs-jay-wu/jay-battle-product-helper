package com.viewsonic.classswift.ui.window.quiz.mvb

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.viewsonic.classswift.R
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.feature.quizcollection.ui.FolderRowKind
import com.viewsonic.classswift.feature.quizcollection.ui.FolderRowUi
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionEvent
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionSidebar
import com.viewsonic.classswift.ui.windowmodel.MvbQuizCollectionWindowModel

/**
 * Compose replacement for the folder sidemenu of MvbQuizCollectionWindow.
 *
 * Renders the SAME [QuizCollectionSidebar] the desktop canvas draws, mapped from the real window
 * UiState — so the shipped sidebar and the design preview are one codebase (replaces the former
 * native RecyclerView + MvbQuizCollectionFolderListAdapter).
 */
@Composable
fun MvbQuizCollectionSidebar(windowModel: MvbQuizCollectionWindowModel) {
    val uiState by windowModel.uiStateFlow.collectAsState()
    val context = LocalContext.current

    // Mirror MvbQuizCollectionFolderListAdapter.buildItems: default folder, then the
    // "Your folders" header, then user folders (only when expanded).
    val rows = buildList {
        val default = uiState.folders.firstOrNull { it.folder.isDefault }
        val rest = uiState.folders.filterNot { it.folder.isDefault }
        default?.let {
            add(FolderRowUi(it.folder.id, it.folder.name, isSelected = it.isSelected, kind = FolderRowKind.DEFAULT))
        }
        add(
            FolderRowUi(
                id = YOUR_FOLDERS_ID,
                name = context.getString(R.string.mvb_qc_your_folders),
                kind = FolderRowKind.YOUR_FOLDERS_HEADER,
                isExpanded = uiState.isYourFoldersExpanded,
            ),
        )
        if (uiState.isYourFoldersExpanded) {
            rest.forEach {
                add(FolderRowUi(it.folder.id, it.folder.name, isSelected = it.isSelected, kind = FolderRowKind.FOLDER))
            }
        }
    }

    AppTheme {
        QuizCollectionSidebar(
            folders = rows,
            onEvent = { event ->
                when (event) {
                    is QuizCollectionEvent.FolderClicked -> windowModel.selectFolder(event.id)
                    QuizCollectionEvent.YourFoldersToggled -> windowModel.toggleYourFoldersExpanded()
                    else -> Unit
                }
            },
            modifier = Modifier.fillMaxSize().padding(horizontal = AppTheme.tokens.spacing.s300),
        )
    }
}

private const val YOUR_FOLDERS_ID = "__your_folders_header__"
