package com.example.paging3pagecache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class MainUiState(
    val statuses: List<Status> = emptyList(),
    val error: String?= null
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://mastodon.social")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service: MastodonService = retrofit.create(MastodonService::class.java)

    init {
        viewModelScope.launch {
            val response = service.publicTimeline(false)
            response.errorBody()
            if (response.isSuccessful) {
                _uiState.update {
                    it.copy(statuses = response.body()!!, error = null)
                }
            } else {
                _uiState.update {
                    it.copy(statuses = emptyList(), error = response.errorBody().toString())
                }
            }
        }
    }
}
