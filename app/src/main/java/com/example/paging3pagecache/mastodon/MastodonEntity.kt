package com.example.paging3pagecache.mastodon

/** A Mastodon status, https://docs.joinmastodon.org/entities/Status/ */
data class Status(
    /**
     * Unique identifier, looks lile a Long, must always be treated as a String, see
     * https://docs.joinmastodon.org/api/guidelines/#id
     */
    val id: String,

    /** Status content */
    val content: String,
)
