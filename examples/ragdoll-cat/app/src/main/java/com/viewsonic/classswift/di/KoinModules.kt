package com.viewsonic.classswift.di

import android.app.ActivityManager
import android.app.Service.LAYOUT_INFLATER_SERVICE
import android.app.Service.WINDOW_SERVICE
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.api.AccountApiService
import com.viewsonic.classswift.api.AiApiService
import com.viewsonic.classswift.api.AuthorizationApiService
import com.viewsonic.classswift.api.BatchQuizApiService
import com.viewsonic.classswift.api.ClassroomApiService
import com.viewsonic.classswift.api.LessonApiService
import com.viewsonic.classswift.api.MaintenanceAnnouncementsApiService
import com.viewsonic.classswift.api.OkHttpBuilder
import com.viewsonic.classswift.api.OtaUpdateApiService
import com.viewsonic.classswift.api.VSApiGateway
import com.viewsonic.classswift.api.QuizApiService
import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.api.RecoverApiService
import com.viewsonic.classswift.api.RetrofitBuilder
import com.viewsonic.classswift.api.SelectOrgApiService
import com.viewsonic.classswift.api.SettingsApiService
import com.viewsonic.classswift.api.UploadFileApiService
import com.viewsonic.classswift.api.amazon.AmazonClient
import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.coordinator.MarkToolHandler
import com.viewsonic.classswift.coordinator.PushTaskCoordinator
import com.viewsonic.classswift.coordinator.RecordMarkHandler
import com.viewsonic.classswift.coordinator.RecordsCoordinator
import com.viewsonic.classswift.coordinator.SketchMarkHandler
import com.viewsonic.classswift.coordinator.SketchTaskCoordinator
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.coordinator.UrlMetaCoordinator
import com.viewsonic.classswift.data.database.TestDatabase
import com.viewsonic.classswift.data.datastore.AccountDataStore
import com.viewsonic.classswift.data.datastore.DebugDataStore
import com.viewsonic.classswift.data.datastore.LoginDataStore
import com.viewsonic.classswift.data.datastore.MaintenanceAnnouncementsDataStore
import com.viewsonic.classswift.data.datastore.QuizDataStore
import com.viewsonic.classswift.data.datastore.SettingsDataStore
import com.viewsonic.classswift.data.datastore.TestDataStore
import com.viewsonic.classswift.data.datastore.TutorialDataStore
import com.viewsonic.classswift.data.datastore.accountDatastore
import com.viewsonic.classswift.data.datastore.debugDatastore
import com.viewsonic.classswift.data.datastore.loginDatastore
import com.viewsonic.classswift.data.datastore.maintenanceAnnouncementsDatastore
import com.viewsonic.classswift.data.datastore.quizDatastore
import com.viewsonic.classswift.data.datastore.settingsDatastore
import com.viewsonic.classswift.data.datastore.testDatastore
import com.viewsonic.classswift.data.datastore.tutorialDataStore
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProviderImpl
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.data.clientapp.myviewboard.transport.MyViewBoardCallbackEmitter
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.handler.TimerHandler
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.AmplitudeManager
import com.viewsonic.classswift.manager.AppUpdateManager
import com.viewsonic.classswift.manager.AudioPlayerHelper
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.FirebaseManager
import com.viewsonic.classswift.manager.FirebaseInstallationManager
import com.viewsonic.classswift.manager.FirebaseRemoteConfigManager
import com.viewsonic.classswift.manager.MvbToolbarStateManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.PendingClassEntryWindowManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.SoundEffectManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.manager.TutorialManager
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.viewmodel.SettingLanguageViewModel
import com.viewsonic.classswift.ui.widgetmodel.RandomDrawWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.RecordMarkWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.quiz.SketchReviewWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.quizcollection.CSCreateQuizCollectionFolderWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.records.RecordsTaskWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.task.ContentTaskWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.task.UrlMetaPreviewDialogWidgetModel
import com.viewsonic.classswift.ui.window.ComingSoonPromptWindow
import com.viewsonic.classswift.ui.window.SelectOrgAndSelectClassWindow
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.ui.window.TutorialWindow
import com.viewsonic.classswift.ui.window.UnderMaintenanceWindow
import com.viewsonic.classswift.ui.window.UpcomingMaintenanceCornerPromptWindow
import com.viewsonic.classswift.ui.window.UpcomingMaintenanceWindow
import com.viewsonic.classswift.ui.window.UpgradePromptWindow
import com.viewsonic.classswift.ui.window.leaderboard.LeaderboardWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbAudioQuizEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbMultipleChoiceEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbPollQuizEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbShortAnswerEditWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbSketchResponseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.mvb.MvbSketchResponseEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbTrueFalseEditWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbAudioQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TextMultipleChoiceStartWindow
import com.viewsonic.classswift.ui.window.task.PushRespondWindow
import com.viewsonic.classswift.ui.windowmodel.LeaderboardWindowModel
import com.viewsonic.classswift.ui.windowmodel.SelectOrgAndSelectClassWindowModel
import com.viewsonic.classswift.ui.windowmodel.JoinClassWindowModel
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.ui.windowmodel.quiz.AudioStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseStartWindowModel
import com.viewsonic.classswift.ui.window.quiz.start.MvbMultipleChoiceStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbPollQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.mvb.MvbQuizCollectionWindow
import com.viewsonic.classswift.ui.windowmodel.MvbQuizCollectionWindowModel
import com.viewsonic.classswift.ui.window.quiz.start.MvbTextShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTextTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.tool.mvb.MvbSpinnerWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizEditWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel
import com.viewsonic.classswift.ui.windowmodel.task.PushRespondWindowModel
import com.viewsonic.classswift.ui.windowmodel.tool.BuzzerWindowModel
import com.viewsonic.classswift.ui.windowmodel.tool.RandomDrawWindowModel
import com.viewsonic.classswift.ui.windowmodel.tool.TimerToolWindowModel
import com.viewsonic.classswift.ui.windowmodel.tool.UnderMaintenanceWindowModel
import com.viewsonic.classswift.ui.windowmodel.tool.spinner.SpinnerWindowModel
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerWindowModel
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.uimanager.maintenance.MaintenancePreDowntimeEligibility
import com.viewsonic.classswift.uimanager.PushRespondUiManager
import com.viewsonic.classswift.uimanager.QuizUiManager
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.localizedContext
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

object KoinModules {
    fun initKoin(applicationContext: Context) {
        startKoin {
            androidContext(applicationContext)
            modules(
                listOf(
                    systemModule,
                    apiServiceModule,
                    socketModule,
                    audioModule,
                    managerModule,
                    uiManagerModule,
                    coordinatorModule,
                    handlerModule,
                    dataStoreModule,
                    databaseModule,
                    csWindowModule,
                    viewModule,
                    windowModule,
                    widgetModelModule,
                    windowModelModule,
                    uncategorizedModule
                )
            )
        }
    }

    /**
     *  System component.
     */
    private val systemModule = module {
        factory { androidContext().getSystemService(WINDOW_SERVICE) as WindowManager }
        factory { androidContext().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater }
        factory { androidContext().getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
        factory { androidContext().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager }
        factory { androidContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
        factory { androidContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
        factory { androidContext().packageManager as PackageManager }
        single { VSApiGateway(androidContext()) }
    }

    /**
     *  For retrofit service
     */
    private val apiServiceModule = module {
        single { OkHttpBuilder().build() }
        single { RetrofitBuilder(BuildConfig.BASE_URL).build() }
        single { get<Retrofit>().create(LessonApiService::class.java) }
        single { get<Retrofit>().create(AccountApiService::class.java) }
        single { get<Retrofit>().create(SelectOrgApiService::class.java) }
        single { get<Retrofit>().create(ClassroomApiService::class.java) }
        single { get<Retrofit>().create(QuizApiService::class.java) }
        single { get<Retrofit>().create(AuthorizationApiService::class.java) }
        single { get<Retrofit>().create(RecoverApiService::class.java) }
        single { RetrofitBuilder(BuildConfig.OTA_UPDATE_URL).build().create(OtaUpdateApiService::class.java) }
        single { RetrofitBuilder(BuildConfig.MAINTENANCE_ANNOUNCEMENT_URL).build().create(MaintenanceAnnouncementsApiService::class.java) }
        single(named("amazon")) { AmazonClient(get()).build() }
        single { get<Retrofit>().create(SettingsApiService::class.java) }
        single { get<Retrofit>().create(UploadFileApiService::class.java) }
        single { get<Retrofit>().create(TaskApiService::class.java) }
        single { get<Retrofit>().create(QuizCollectionApiService::class.java) }
        single { get<Retrofit>().create(AiApiService::class.java) }
        single { get<Retrofit>().create(BatchQuizApiService::class.java) }
    }

    /**
     *  For socket service
     */
    private val socketModule = module {
        single { SocketManager(get(), get()) }
    }

    /**
     *  For Audio service
     */
    private val audioModule = module {
        factory {
            AudioPlayerHelper(androidContext().localizedContext())
        }
    }

    /**
     *  For manager
     */
    private val managerModule = module {
        single { ToolbarManager(get(), get(), get(), get(), get()) }
        single { AmplitudeManager(get(), get()) }
        single { NetworkManager(get()) }
        single { FirebaseInstallationManager() }
        single { FirebaseRemoteConfigManager() }
        single { FirebaseManager(get(), get()) }
        single { AppUpdateManager(get(), get(), get(), get()) }
        single<MyViewBoardConnectionStateProvider> { MyViewBoardConnectionStateProviderImpl() }
        single { MyViewBoardCallbackEmitter() }
        single { MyViewBoardEventNotifier(get(), get()) }
        single { AccountManager(androidContext().localizedContext(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { StudentManager(androidContext().localizedContext(), get(), get(), get(), get()) }
        single { ClassroomManager(get(), get(), get(), get()) }
        single { QuizManager(androidContext().localizedContext(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { SoundEffectManager(androidContext().localizedContext()) }
        single { TutorialManager(get()) }
        single { ScreenshotManager(get(), get()) }
        single { BatchQuizManager(get(), get(), get()) }
        single { MvbToolbarStateManager() }
        single { PendingClassEntryWindowManager() }
    }

    /**
     * UiManager should only be used in the UI layer. Do not inject or use it in the Domain or Data layers.
     */
    private val uiManagerModule = module {
        single { QuizUiManager(get()) }
        single { PushRespondUiManager(get(), get(), get(), get(), get(), get(), get()) }
        factory { MaintenancePreDowntimeEligibility(get()) }
        single { MaintenanceAnnouncementsUiManager(get(), get(), get(), get(), get(), get(), get()) }
        single { UnclosedMissionUiManager() }
    }

    private val coordinatorModule = module {
        factory { PushTaskCoordinator(get(), get(), get(), get(), get()) }
        factory { RecordsCoordinator(get(), get(), get(), get(), get(), get()) }
        // VSFT-8453 / 8454 Sketch Response 結果頁專屬 task lifecycle coordinator
        factory { SketchTaskCoordinator(get(), get(), get(), get(), get(), get()) }
        factory { UrlMetaCoordinator(get(), get()) }
    }

    private val handlerModule = module {
        factory { UploadFileHandler(get(), get(), get(), get()) }
        factory { MarkToolHandler(get()) }
        factory { RecordMarkHandler(get(), get()) }
        // VSFT-8454 Sketch Response 批改 + hand back handler
        factory { SketchMarkHandler(get(), get()) }
        factory { TimerHandler() }
    }

    private val dataStoreModule = module {
        single { TestDataStore(androidContext().testDatastore) }
        single { LoginDataStore(androidContext().loginDatastore) }
        single { QuizDataStore(androidContext().quizDatastore) }
        single { TutorialDataStore(androidContext().tutorialDataStore) }
        single { AccountDataStore(androidContext().accountDatastore) }
        single { MaintenanceAnnouncementsDataStore(androidContext().maintenanceAnnouncementsDatastore) }
        single { DebugDataStore(androidContext().debugDatastore) }
        single { SettingsDataStore(androidContext().settingsDatastore) }
    }

    private val databaseModule = module {
        single { TestDatabase.build(androidContext()) }
    }

    private val viewModule = module {
        viewModel { LoginViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { SettingLanguageViewModel(get()) }
    }

    // ui using applicationContext, please using factory not single,
    private val windowModule = module {
        factory { ComingSoonPromptWindow(androidContext().localizedContext()) }
        factory { UpgradePromptWindow(androidContext().localizedContext()) }
        factory { JoinClassWindow(androidContext().localizedContext()) }
        factory { SelectOrgAndSelectClassWindow(androidContext().localizedContext()) }
        factory { MvbShortAnswerEditWindow(androidContext().localizedContext()) }
        factory { MvbTrueFalseEditWindow(androidContext().localizedContext()) }
        factory { MvbMultipleChoiceEditWindow(androidContext().localizedContext()) }
        factory { MvbTrueFalseStartWindow(androidContext().localizedContext()) }
        factory { MvbTextTrueFalseStartWindow(androidContext().localizedContext()) }
        factory { MvbMultipleChoiceStartWindow(androidContext().localizedContext()) }
        factory { MvbShortAnswerStartWindow(androidContext().localizedContext()) }
        factory { MvbTextShortAnswerStartWindow(androidContext().localizedContext()) }
        factory { MvbSketchResponseEditWindow(androidContext().localizedContext()) }
        factory { MvbSketchResponseStartWindow(androidContext().localizedContext()) }
        factory { MvbAudioQuizEditWindow(androidContext().localizedContext()) }
        factory { TutorialWindow(androidContext().localizedContext()) }
        factory { MvbAudioQuizStartWindow(androidContext().localizedContext()) }
        factory { PushRespondWindow(androidContext().localizedContext()) }
        factory { UpcomingMaintenanceWindow(androidContext().localizedContext()) }
        factory { UpcomingMaintenanceCornerPromptWindow(androidContext().localizedContext()) }
        factory { UnderMaintenanceWindow(androidContext().localizedContext()) }
        factory { LeaderboardWindow(androidContext().localizedContext()) }
        factory { MvbQuizCollectionWindow(androidContext().localizedContext()) }
        factory { MvbSpinnerWindow(androidContext().localizedContext()) }
        factory { MvbPollQuizEditWindow(androidContext().localizedContext()) }
        factory { MvbPollQuizStartWindow(androidContext().localizedContext()) }
        factory { TextMultipleChoiceStartWindow(androidContext().localizedContext()) }
    }

    private val widgetModelModule = module {
        factory { RandomDrawWidgetModel(get(), get(), get(), get()) }
        factory { ContentTaskWidgetModel(get(), get()) }
        factory { RecordsTaskWidgetModel(get(), get(), get(), get(), get(), get()) }
        factory { UrlMetaPreviewDialogWidgetModel(get()) }
        factory { RecordMarkWidgetModel(get(), get(), get()) }
        // VSFT-8454 Sketch Response 批改 widget model
        factory { SketchReviewWidgetModel(androidContext(), get(), get()) }
        factory { CSCreateQuizCollectionFolderWidgetModel(get(), get()) }
    }

    private val windowModelModule = module {
        factory { SelectOrgAndSelectClassWindowModel(androidContext().localizedContext(), get(), get(), get(), get(), get(), get()) }
        factory { JoinClassWindowModel(get(), get(), get(), get(), get(), get(), get(), get()) }
        factory { QuizCommonWindowModel(get(), get(), get(), get()) }
        factory { QuizEditWindowModel(get(), get(), get(), get()) }
        factory { MvbSketchResponseEditWindowModel(get(), get(), get(), get(), get()) }
        factory { QuizStartWindowModel(androidContext().localizedContext(), get(), get()) }
        factory { TrueFalseWindowModel(get()) }
        factory { MultipleChoiceWindowModel(get()) }
        factory { TimerToolWindowModel() }
        factory { RandomDrawWindowModel(get(), get(), get()) }
        factory { BuzzerWindowModel(get(), get(), get(), get(), get(), get()) }
        factory { SpinnerWindowModel(get(), get(), get()) }
        factory { MvbSpinnerWindowModel(get(), get(), get(), get(), get()) }
        factory {
            val accountManager: com.viewsonic.classswift.manager.AccountManager = get()
            val classroomManager: com.viewsonic.classswift.manager.ClassroomManager = get()
            MvbSketchResponseStartWindowModel(
                taskApiService = get(),
                networkManager = get(),
                tokenProvider = { accountManager.getBearerToken() },
                lessonIdProvider = { classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId },
                coordinator = get(),
                markHandler = get(),
            )
        }
        factory { AudioStartWindowModel(get(), get()) }
        factory { PushRespondWindowModel(get(), get(), get(), get()) }
        factory { UnderMaintenanceWindowModel(get()) }
        factory {
            MvbQuizCollectionWindowModel(
                androidContext().localizedContext(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        factory { LeaderboardWindowModel(get(), get(), get()) }
    }

    /**
     * For floating window manager
     */
    private val csWindowModule = module {
        single { CSWindowManager }
    }

    /**
     *  uncategorized component.
     */
    private val uncategorizedModule = module {
        single {
            Firebase.analytics
        }
        single {
            MoshiProvider
        }
        single {
            AmplitudeFactory(get())
        }
    }
}
