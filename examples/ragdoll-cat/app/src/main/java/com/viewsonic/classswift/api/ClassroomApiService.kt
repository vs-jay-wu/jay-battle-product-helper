package com.viewsonic.classswift.api

import com.viewsonic.classswift.constant.ApiConstant
import com.viewsonic.classswift.api.body.CreateRoomBody
import com.viewsonic.classswift.api.body.GuestRoomBody
import com.viewsonic.classswift.api.body.UpdateRoomBody
import com.viewsonic.classswift.api.response.CreateLessonResponse
import com.viewsonic.classswift.api.response.CreateRoomResponse
import com.viewsonic.classswift.api.response.DeleteRoomResponse
import com.viewsonic.classswift.api.response.GetGuestRoomResponse
import com.viewsonic.classswift.api.response.GetRoomsResponse
import com.viewsonic.classswift.api.response.UnclosedMissionsResponse
import com.viewsonic.classswift.api.response.UpdateRoomResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ClassroomApiService {
    //    Get Class List
    @GET("api/v3/rooms")
    suspend fun getRooms(
        @Header("Authorization") token: String,
        @Query("org_id") orgId: String,
        @Query("user_id") userId: String,
        @Query("sort_type") sortType: String = ApiConstant.RoomSortType.LATEST_ACTIVITY.queryValue,
    ): ApiResponse<GetRoomsResponse>

    @POST("/api/v3/rooms")
    suspend fun createRooms(@Header("Authorization")token : String, @Query("student_list")studentList:Boolean,  @Query("grouping")grouping : Boolean ,@Body body : CreateRoomBody) : ApiResponse<CreateRoomResponse>

    //    Delete Class
    @DELETE("/api/v3/rooms/{room_id}")
    suspend fun deleteRooms(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: String
    ): ApiResponse<DeleteRoomResponse>

    //    Update Class
    @PUT("/api/v3/rooms/{room_id}")
    suspend fun updateRooms(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: String,
        @Body body : UpdateRoomBody
    ): ApiResponse<UpdateRoomResponse>

    //    Create Lesson
    @POST("/api/v3/rooms/{room_id}/lessons")
    suspend fun createLesson(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: String
    ): ApiResponse<CreateLessonResponse>

    @GET("/api/v3/lessons/{lesson_id}/unclosed_missions")
    suspend fun getUnclosedMissions(
        @Header("Authorization")token : String,
        @Path("lesson_id") lessonId : String
    ): ApiResponse<UnclosedMissionsResponse>

    //    Get Guest Class List
    @POST("/api/v3/guest/room")
    suspend fun getGuestRoom(
        @Header("Authorization") token: String,
        @Body body: GuestRoomBody
    ): ApiResponse<GetGuestRoomResponse>
}
