package com.example.paging3pagecache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.paging3pagecache.mastodon.MastodonService
import com.example.paging3pagecache.mastodon.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://mastodon.social")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service: MastodonService = retrofit.create(MastodonService::class.java)
    private val repository = TimelineRepository(service)

    val statuses = repository.getStatusStream(viewModelScope).cachedIn(viewModelScope)
}
