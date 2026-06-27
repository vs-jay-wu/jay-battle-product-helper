package com.viewsonic.classswift.manager

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.viewsonic.classswift.data.datastore.TutorialDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TutorialManager(
    private val tutorialDataStore: TutorialDataStore
) {
    // 當使用者看完 Class 頁面的 Tooltips，但沒有進入到 Student List 頁面就不算真正完成。
    // 但仍需要讓使用者下次進 Class 頁面不用再看一次 Tooltips，如果 App 重開就要讓使用者再看一次。
    private val tutorialClassListPhaseLookedMap: MutableMap<String, Boolean> = ConcurrentMutableMap()

    private var _userId: String = ""
    
    fun setUserId(id: String) {
        _userId = id
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        tutorialDataStore.clearClassPagePhaseCompletion()
        tutorialDataStore.clearStudentPagePhaseCompletion()
    }

    suspend fun clearUserData() = withContext(Dispatchers.IO) {
        tutorialClassListPhaseLookedMap.clear()
        _userId = ""
    }

    suspend fun isClassPagePhaseCompleted(): Boolean = withContext(Dispatchers.IO) {
        return@withContext tutorialDataStore.isClassPagePhaseCompleted(_userId)
    }

    suspend fun isClassPagePhaseLooked(): Boolean = withContext(Dispatchers.IO) {
        return@withContext tutorialClassListPhaseLookedMap[_userId] == true
    }

    suspend fun setClassPagePhaseLooked() = withContext(Dispatchers.IO) {
        tutorialClassListPhaseLookedMap[_userId] = true
    }
}