package com.example.paging3pagecache

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

public interface MastodonService {
    @GET("api/v1/timelines/public")
    suspend fun publicTimeline(
        @Query("local") local: Boolean? = null,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Status>>
}
