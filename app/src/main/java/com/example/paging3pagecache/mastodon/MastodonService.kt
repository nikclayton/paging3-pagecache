package com.example.paging3pagecache.mastodon

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

public interface MastodonService {
    @GET("api/v1/timelines/public")
    suspend fun publicTimeline(
        @Query("local") local: Boolean? = null,
        @Query("max_id") maxId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Status>>
}

/** Models next/prev links from the "Links" header in an API response */
data class Links(val next: String?, val prev: String?) {
    companion object {
        fun from(linkHeader: String?): Links {
            val links = HttpHeaderLink.parse(linkHeader)
            return Links(
                next = HttpHeaderLink.findByRelationType(links, "next")?.uri?.getQueryParameter(
                    "max_id"
                ),
                prev = HttpHeaderLink.findByRelationType(links, "prev")?.uri?.getQueryParameter(
                    "min_id"
                )
            )
        }
    }
}
