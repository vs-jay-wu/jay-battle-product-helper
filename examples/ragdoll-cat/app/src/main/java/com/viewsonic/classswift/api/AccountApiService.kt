package com.viewsonic.classswift.api

import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.api.body.GuestLoginPostBody
import com.viewsonic.classswift.api.body.IsFeedbackHelpedBody
import com.viewsonic.classswift.api.body.LoginPostNoTokenBody
import com.viewsonic.classswift.api.body.LoginWithMvbTokenPostBody
import com.viewsonic.classswift.api.body.LoginWithRefreshTokenPostBody
import com.viewsonic.classswift.api.body.UserInfoBody
import com.viewsonic.classswift.api.response.AccountInfoResponse
import com.viewsonic.classswift.api.response.ArticleResponse
import com.viewsonic.classswift.api.response.ChallengeCodeResponse
import com.viewsonic.classswift.api.response.FeedbackResponse
import com.viewsonic.classswift.api.response.GuestLoginResponse
import com.viewsonic.classswift.api.response.LoginResponse
import com.viewsonic.classswift.api.response.LoginUrlsResponse
import com.viewsonic.classswift.api.response.OAuthCodeResponse
import com.viewsonic.classswift.api.response.TeacherStateResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AccountApiService {
    @GET("/api/v3/auth/pkce")
    suspend fun getChallengeCode(): ApiResponse<ChallengeCodeResponse>

    @POST("/api/v3/account/token/login")
    suspend fun loginWithMvbToken(
        @Body body: LoginWithMvbTokenPostBody
    ): ApiResponse<LoginResponse>

    @POST("/api/v3/account/login")
    suspend fun login(
        @Body body: LoginWithRefreshTokenPostBody
    ): ApiResponse<LoginResponse>

    @POST("/api/v3/account/login")
    suspend fun login(
        @Body body: LoginPostNoTokenBody
    ): ApiResponse<LoginResponse>

    @GET("/api/v3/account/info")
    suspend fun getAccountInfo(@Header("Authorization") token: String): ApiResponse<AccountInfoResponse>

    @GET("/api/v3/user_consent")
    suspend fun getArticleID(): ApiResponse<ArticleResponse>

    @PUT("/api/v3/user/user_info")
    suspend fun sendAccountInfo(@Header("Authorization") token: String, @Body body : UserInfoBody): ApiResponse<String>

    @GET("/api/v3/organization/{org_id}/teacher_state")
    suspend fun getTeacherState(
        @Header("Authorization") token: String,
        @Path("org_id") orgId: String,
        @Query("user_id") userId: String
    ): ApiResponse<TeacherStateResponse>

    @GET("/api/v3/sso/viewsonic/login_urls")
    suspend fun getSsoSignInUrls(
        @Query("sso_types") ssoTypes: String,
        @Query("code_challenge") codeChallenge: String,
        @Query("redirect_uri") redirectUrl: String = BuildConfig.LOGIN_REDIRECT_URL,
        @Query("client") client: String = "ANDROID"
    ): ApiResponse<LoginUrlsResponse>

    @GET("/api/v3/sso/viewsonic/qr_code_url")
    suspend fun getQrCodeSignInUrl(
        @Query("code_challenge") codeChallenge: String = "",
        @Query("state") state: String = "",
        @Query("redirect_uri") redirectUrl: String = BuildConfig.QR_CODE_REDIRECT_URL,
        @Query("client") client: String = "ANDROID"
    ): ApiResponse<LoginUrlsResponse>

    @GET("/api/v3/sso/viewsonic/auth_code")
    suspend fun getOAuthCode(@Query("code_challenge") codeChallenge: String): ApiResponse<OAuthCodeResponse>

    /**
     *  For in-app tutorial used
     */
    @GET("/api/v3/tutorial/feedback")
    suspend fun getFeedbackContent(@Header("Authorization") token: String): ApiResponse<FeedbackResponse>

    /**
     *  For in-app tutorial used
     */
    @POST("/api/v3/tutorial/feedback")
    suspend fun sendIsFeedbackHelped(
        @Header("Authorization") token: String,
        @Body body: IsFeedbackHelpedBody
    ): ApiResponse<ResponseBody>

    @POST("/api/v3/guest/login")
    suspend fun guestLogin(
        @Header("x-api-key") apiKey: String,
        @Body body: GuestLoginPostBody
    ): ApiResponse<GuestLoginResponse>
}
