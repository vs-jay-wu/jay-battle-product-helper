package com.viewsonic.classswift.data.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.api.response.AiSubjectDisplayNamesData
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.manager.AccountManager

class BatchQuizzesPagingSource(
    private val folderId: String,
    private val accountManager: AccountManager,
    private val quizCollectionApiService: QuizCollectionApiService,
    private val aiSubjectDisplayNamesDataList: List<AiSubjectDisplayNamesData>
) : PagingSource<Int, QuizInCollectionInfo>() {

    override fun getRefreshKey(state: PagingState<Int, QuizInCollectionInfo>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, QuizInCollectionInfo> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            val response = quizCollectionApiService.getQuizzesInCollectionFolder(
                token = accountManager.getBearerToken(),
                folderId = folderId,
                origin = null,
                sourceTypes = listOf("IMPORT_CONTENT"),
                page = page,
                perPage = pageSize,
                quizType = "TRUE_FALSE,SINGLE_SELECT,MULTIPLE_SELECT",
                orderBy = "QUIZ_TYPE"
            )

            when (response) {
                is ApiResponse.Success -> {
                    LoadResult.Page(
                        data = response.data.quizDataList.map { quizData ->
                            QuizInCollectionInfo(
                                quizData = quizData,
                                subjectDisplayName = aiSubjectDisplayNamesDataList
                                    .find { it.country == quizData.country }
                                    ?.subjects
                                    ?.find { it.key == quizData.subject }
                                    ?.displayName
                                    ?: quizData.subject
                            )
                        },
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (response.data.quizDataList.isEmpty()) null else page + 1
                    )
                }

                else -> {
                    LoadResult.Error(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}