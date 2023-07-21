package com.example.paging3pagecache.mastodon

import com.google.gson.annotations.SerializedName

data class Status(
    val id: String,
    val content: String,
    val sensitive: Boolean,
    @SerializedName("spoiler_text") val spoilerText: String,
)
